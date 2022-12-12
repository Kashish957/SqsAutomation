package handler;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.util.List;

public class LambdaHandler implements RequestHandler<Object, Object> {
    private static final String simpleQueue = "DLQ_QR.fifo";
    private AWSCognitoIdentityProvider client;

    final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

    public Object handleRequest(Object o, Context c) {
        client = createCognitoClient();
        String dlqQueueUrl = sqs.getQueueUrl(simpleQueue).getQueueUrl();
        List<Message> messages = getMessagesFromQueue(dlqQueueUrl);
         while(messages.size()>0) {
            for (Message msg : messages) {
                try {
                    String[] l = msg.getBody().split("\"");
                    String email="";
                    String subId="";
                    for (int i = 0; i < l.length; i++) {
                        subId = (l[i].contains("sub"))?l[i + 8]:subId;
                        email=(l[i].contains("email"))?l[i + 8]:email;
                    }

                    String primaryUserSubId=checkEmailInCognito(email);
                    if(subId!=primaryUserSubId)
                    {
                        String msgBody = msg.getBody();
                        String msgb= msgBody.replace(subId,primaryUserSubId);
                       msg.setBody(msgb);
                    }


                    SendMessageRequest send_msg_request = new SendMessageRequest()
                            .withQueueUrl("https://sqs.us-east-1.amazonaws.com/601067240558/DLQ_MAIN.fifo")
                            .withMessageBody(msg.getBody());
                    send_msg_request.setMessageGroupId("1");
                    send_msg_request.setMessageDeduplicationId(msg.getMessageId());

                    sqs.sendMessage(send_msg_request);
                    sqs.deleteMessage(dlqQueueUrl,msg.getReceiptHandle());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
             messages=getMessagesFromQueue(dlqQueueUrl);
         }
        return null;
    }

    public List<Message> getMessagesFromQueue(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        return sqs.receiveMessage(receiveMessageRequest).getMessages();
    }
    public void saveMessagesBackup()
    {}

    private AWSCognitoIdentityProvider createCognitoClient() {
        AWSCredentials cred = new BasicAWSCredentials("AKIAYX4TGHRXJAUUJRXZ", "H60uNddl0XPGlTMj5U7f0c+/dKZpM0STTR8tg0nd");
        AWSCredentialsProvider credProvider = new AWSStaticCredentialsProvider(cred);
        return AWSCognitoIdentityProviderClientBuilder.standard().withCredentials(credProvider).withRegion(Regions.US_EAST_1).build();
    }
    public String checkEmailInCognito(String email) {
        AdminGetUserRequest x = new AdminGetUserRequest();
        x.setUserPoolId("us-east-1_1ThF1Ac1b");
        x.setUsername(email);
        AdminGetUserResult y=client.adminGetUser(x);
        return y.getUsername();
    }
}
