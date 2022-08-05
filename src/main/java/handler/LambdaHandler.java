package handler;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;
public class LambdaHandler implements RequestHandler<Object, Object>
{
    private static final String simpleQueue = "dead-letter_queue.fifo";
    final AmazonSQS sqs = AmazonSQSClientBuilder.standard().build();
    public Object handleRequest(Object i, Context o)
    {
        String queueUrl = getQueueUrl(simpleQueue);
        System.out.println(queueUrl);
        List<Message> messages = getMessagesFromQueue(queueUrl);
        System.out.println("Below are the Messages present in DLQ:---");
        for (Message msg : messages)
        {
            try
            {
                //System.out.println(msg);
                // System.out.println("this are
                // messageAttributes"+msg.getMessageAttributes().getClass().getName());
                // System.out.println(msg.getBody().getClass());

                // System.out.println("Timestamp is ");
                // System.out.println(msg.getClass().getField("Body").getClass().getField("Timestamp"));
                JSONObject msgBody = new JSONObject(msg.getBody().replaceAll(" ", ""));
                JSONObject msgAttr = new JSONObject(msgBody.get("MessageAttributes").toString());
                Object timestamp = msgBody.get("Timestamp");
                String email = (new JSONObject(msgAttr.get("email").toString())).get("Value").toString();
                String subId = (new JSONObject(msgAttr.get("sub").toString())).get("Value").toString();
                System.out.println(subId + "\n" + email + "\n" + timestamp);
                /*checkEmailInCognito(email);*/
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }
    public String getQueueUrl(String queueName) {
        GetQueueUrlRequest getQueueUrlRequest = new GetQueueUrlRequest(queueName);
        return sqs.getQueueUrl(getQueueUrlRequest).getQueueUrl();
    }

    public List<Message> getMessagesFromQueue(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
        return messages;
    }
}
