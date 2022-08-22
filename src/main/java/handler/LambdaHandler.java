package handler;

import com.amazonaws.auth.*;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.ListUsersRequest;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class LambdaHandler implements RequestHandler<Object, Object> {
    private static final String simpleQueue = "dead-letter_queue.fifo";
    private AWSCognitoIdentityProvider client;

    final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

    public Object handleRequest(Object o, Context c) {
        client = createCognitoClient();
        String queueUrl = sqs.getQueueUrl(simpleQueue).getQueueUrl();
        System.out.println(queueUrl);
        List<Message> messages = getMessagesFromQueue(queueUrl);
        System.out.println("Below are the Messages present in DLQ:---");
        for (Message msg : messages) {
            try {
                //System.out.println(msg.getBody().replaceAll("&nbsp;", ""));
                //msg.getBody().strip()
                String[] l=msg.getBody().split(":");
                String email="";
                String sub="";
                String Timestamp="";
                for(int i=0;i<l.length;i++)
                {
                    if(l[i].contains("\"Timestamp\"")){
                        System.out.println("Timestamp is "+l[i+1]);
                    }
                    if(l[i].contains("\"sub\""))
                    {
                        //sub=l[i+3].substring(1,l[i+3].length()-8);
                        System.out.println("sub is "+l[i+3]);

                    }
                    if(l[i].contains("\"email\""))
                    {
                        email=l[i+3].substring(1,l[i+3].length()-8);
                        System.out.println("email is "+email);


                    }


                }

               /* JSONObject msgBody = new JSONObject(msg.getBody().replaceAll(" ", ""));
                JSONObject msgAttr = new JSONObject(msgBody.get("MessageAttributes"));
                Object timestamp = msgBody.get("Timestamp");
                String email = (new JSONObject(msgAttr.get("email").toString())).get("Value").toString();
                String subId = (new JSONObject(msgAttr.get("sub").toString())).get("Value").toString();
                System.out.println(subId + "\n" + email + "\n" + timestamp);*/
                checkEmailInCognito(email);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    public List<Message> getMessagesFromQueue(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        return sqs.receiveMessage(receiveMessageRequest).getMessages();
    }

    private AWSCognitoIdentityProvider createCognitoClient() {
        AWSCredentials cred = new BasicAWSCredentials("AKIAXJ752B2RV23BZUHH", "9iFkz5jjTq90CaIW/eSZ9B4gW6G5Zj6eDXpUe+sv");
        AWSCredentialsProvider credProvider = new AWSStaticCredentialsProvider(cred);
        return AWSCognitoIdentityProviderClientBuilder.standard().withCredentials(credProvider).withRegion(Regions.US_EAST_1).build();
    }

    public void checkEmailInCognito(String email) {
        String filter = "email = \"" + email + "\"";
        ListUsersRequest l = new ListUsersRequest();
        l.setAttributesToGet(new ArrayList<>());
        l.setFilter(filter);
        l.setUserPoolId("us-east-1_DJpISoGFK");
        ListUsersResult result = client.listUsers(l);
        result.getUsers().forEach(user -> System.out.println("User with filter applied " + user.getUsername() + " Status " + user.getUserStatus()
                + " Created " + user.getUserCreateDate()));
    }
}
