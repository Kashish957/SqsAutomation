import java.util.List;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Regions;

/*import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;*/
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


public class Fetch_Messages implements RequestHandler<Object, Object> {
    // private BasicAWSCredentials credentials;
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

   /* public String checkEmailInCognito(String email) {
	//CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder().region(Region.US_EAST_1)
		//.credentialsProvider(ProfileCredentialsProvider.create()).build();
	// AWSCognitoIdentityProvider cognitoClient =
	// AWSCognitoIdentityProviderClientBuilder.standard()
	// .withCredentials(awsCredentialsProvider).build();
	String filter = "email = \"" + email + "\"";
	ListUsersRequest usersRequest = ListUsersRequest.builder().userPoolId(" us-east-1_DJpISoGFK").filter(filter)
		.build();
	ListUsersResponse response = ((CognitoIdentityProviderClient) cognitoClient).listUsers(usersRequest);
	response.users().forEach(user -> {
	    System.out.println("User with filter applied " + user.username() + " Status " + user.userStatus()
		    + " Created " + user.userCreateDate());
	});
	return null;
    }*/



    public String getQueueUrl(String queueName) {
        GetQueueUrlRequest getQueueUrlRequest = new GetQueueUrlRequest(queueName);
        return sqs.getQueueUrl(getQueueUrlRequest).getQueueUrl();
    }

    public List<Message> getMessagesFromQueue(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
        return messages;
    }

    // private static volatile AWSSimpleQueueServiceUtil awssqsUtil = new
    // AWSSimpleQueueServiceUtil();
    /*
     * private void AWSSimpleQueueServiceUtil(){ try{ Properties properties = new
     * Properties(); properties.load(new FileInputStream(
     * "D:/samayik/adkonnection/src/main/resources/AwsCredentials.properties"));
     * this.credentials = new
     * BasicAWSCredentials(properties.getProperty("accessKey"),
     * properties.getProperty("secretKey"));
     *
     * this.sqs = new AmazonSQSClient(this.credentials);
     *
     * // this.sqs.setEndpoint("https://sqs.ap-southeast-1.amazonaws.com");
     *
     * //AmazonSQS sqs = new AmazonSQSClient(new
     * ClasspathPropertiesFileCredentialsProvider());
     *
     * }catch(Exception e){
     * System.out.println("exception while creating awss3client : " + e); } } public
     * String setSqsClient() { BasicAWSCredentials bAWSc = new
     * BasicAWSCredentials(accessKey, secretKey); return
     * AmazonSQSClientBuilder.standard().withRegion(region).withCredentials(new
     * AWSStaticCredentialsProvider(bAWSc)).build(); }
     */
}
