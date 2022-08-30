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
       // while(messages.size()>0) {
            for (Message msg : messages) {
                try {
                    String[] l = msg.getBody().split("\"");
                    String email="";
                    String sub="";
                    String Timestamp="";
                    for (int i = 0; i < l.length; i++) {
                        Timestamp = (l[i].contains("Timestamp"))?l[i + 2]:Timestamp;
                        sub = (l[i].contains("sub"))?l[i + 8]:sub;
                        email=(l[i].contains("email"))?l[i + 8]:email;
                    }
                    System.out.println("Timestamp is " + Timestamp);
                    System.out.println("sub is " + sub);
                    System.out.println("email is " + email);

                    checkEmailInCognito(email);
                    //if email exist and user is not primary replace subid with primary users subid

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
       // }
        return null;
    }
    public void checkEmailInCognito(String email) {
        String filter = "email = \"" + email + "\"";
        ListUsersRequest l = new ListUsersRequest();
        l.setAttributesToGet(new ArrayList<>());
        l.setFilter(filter);
        l.setUserPoolId("us-east-1_DJpISoGFK");
        ListUsersResult result = client.listUsers(l);
        result.getUsers().forEach(user ->
        {
            System.out.println("User with filter applied " + user.getUsername() + " Status " + user.getUserStatus()
                    + " Created " + user.getUserCreateDate());


        });
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
