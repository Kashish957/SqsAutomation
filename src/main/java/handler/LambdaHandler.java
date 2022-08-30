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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class LambdaHandler implements RequestHandler<Object, Object> {
    private static final String simpleQueue = "dead-letter_queue.fifo";
    private AWSCognitoIdentityProvider client;

    final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

    public Object handleRequest(Object o, Context c) {
        client = createCognitoClient();
        String queueUrl = sqs.getQueueUrl(simpleQueue).getQueueUrl();
        List<Message> messages = getMessagesFromQueue(queueUrl);
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
                    String primaryUserSubId=getPrimaryUserSubId(email);
                    if(subId!=primaryUserSubId)
                    {
                        String msgBody = msg.getBody();

                        //"sub"(.)*} -> "sub":type.....
                        msgBody.replace("\\^sub.*}",primaryUserSubId);
                    }// how to find index of } after "sub" ..
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
       // }
        return null;
    }
    public String getPrimaryUserSubId(String email) {
        String filter = "email = \"" + email + "\"";
        ListUsersRequest l = new ListUsersRequest();
        l.setAttributesToGet(new ArrayList<>());
        l.setFilter(filter);
        l.setUserPoolId("us-east-1_DJpISoGFK");
       List<UserType> result = client.listUsers(l).getUsers();
        AtomicReference<Date> oldestDate= new AtomicReference<>(new Date());
        AtomicReference<String> oldestUserSubId= new AtomicReference<>("");
        result.forEach(user ->
        {
            System.out.println("User with filter applied " + user.getUsername() + " Status " + user.getUserStatus()
                    + " Created " + user.getUserCreateDate());
            Date createdDate = user.getUserCreateDate();
            if(oldestDate.get().compareTo(createdDate)<0)
            {
                oldestDate.set(createdDate);
                oldestUserSubId.set(user.getUsername());
            }
        });
        return oldestUserSubId.toString();

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
