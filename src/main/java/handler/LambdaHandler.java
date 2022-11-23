package handler;

import com.amazonaws.auth.*;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.ListUsersRequest;
import com.amazonaws.services.cognitoidp.model.UserType;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class LambdaHandler implements RequestHandler<Object, Object> {
    private static final String simpleQueue = "dead-letter_queue.fifo";
    private AWSCognitoIdentityProvider client;

    final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

    public Object handleRequest(Object o, Context c) {
        client = createCognitoClient();
        String dlqQueueUrl = sqs.getQueueUrl(simpleQueue).getQueueUrl();
        List<Message> messages = getMessagesFromQueue(dlqQueueUrl);
        //String mainQueueUrl = sqs.getQueueUrl("main_queue.fifo").getQueueUrl();
        System.out.println("Below are the Messages present in DLQ:---");
       // while(messages.size()>0) {
            for (Message msg : messages) {
                try {
                    String[] l = msg.getBody().split("\"");
                    String email="";
                    String subId="";
                    String Timestamp="";
                    for (int i = 0; i < l.length; i++) {
                        Timestamp = (l[i].contains("Timestamp"))?l[i + 2]:Timestamp;
                        subId = (l[i].contains("sub"))?l[i + 8]:subId;
                        email=(l[i].contains("email"))?l[i + 8]:email;
                    }
                    System.out.println("Timestamp is " + Timestamp);
                    System.out.println("sub is " + subId);
                    System.out.println("email is " + email);
                    String primaryUserSubId=getPrimaryUserSubIdbyCmd(email);
                    if(subId!=primaryUserSubId)
                    {
                        String msgBody = msg.getBody();
                        String msgb= msgBody.replace(subId,primaryUserSubId);
                       msg.setBody(msgb);
                       System.out.println(msgb);
                    }

                    /*SendMessageRequest send_msg_request = new SendMessageRequest()
                            .withQueueUrl(mainQueueUrl)
                            .withMessageBody(msg.getBody())
                            .withDelaySeconds(5);
                    sqs.deleteMessage(dlqQueueUrl,msg.getReceiptHandle());
                    sqs.sendMessage(send_msg_request);*/

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
       // }
        return null;
    }

    public String getPrimaryUserSubIdbyCmd(String email)
    {
        try
        {
            String[] commands = {"cmd",  "/K", "aws cognito-idp admin-get-user --user-pool-id us-east-1_76MUNFQpM --username "+email+" --profile ecim-read --region us-east-1"};

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(Runtime.getRuntime().exec(commands).getInputStream()));

            while (!(stdInput.readLine().trim().equals("\"Name\": \"sub\","))) {}
            String s= stdInput.readLine().trim();
            return s.substring(10,s.length()-1);
        }
        catch (Exception e)
        {
            System.out.println("HEY Buddy ! U r Doing Something Wrong ");
            e.printStackTrace();
        }
        return "";
    }
    public List<Message> getMessagesFromQueue(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        return sqs.receiveMessage(receiveMessageRequest).getMessages();
        //return sqs.receiveMessage(queueUrl)
    }
    public void saveMessagesBackup()
    {}

    private AWSCognitoIdentityProvider createCognitoClient() {
        AWSCredentials cred = new BasicAWSCredentials("AKIAXJ752B2RV23BZUHH", "9iFkz5jjTq90CaIW/eSZ9B4gW6G5Zj6eDXpUe+sv");
        AWSCredentialsProvider credProvider = new AWSStaticCredentialsProvider(cred);
        return AWSCognitoIdentityProviderClientBuilder.standard().withCredentials(credProvider).withRegion(Regions.US_EAST_1).build();
    }
}
