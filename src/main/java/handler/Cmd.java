package handler;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Cmd {
    public static void main(String[] args)
    {
        try
        {
            String[] commands = {"cmd",  "/K", "aws cognito-idp admin-get-user --user-pool-id us-east-1_76MUNFQpM --username test.test@sn.com --profile ecim-read --region us-east-1"};

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(Runtime.getRuntime().exec(commands).getInputStream()));

            while (!(stdInput.readLine().trim().equals("\"Name\": \"sub\","))) {}
            String s= stdInput.readLine().trim();
            System.out.println(s.substring(10,s.length()-1));

        }
        catch (Exception e)
        {
            System.out.println("HEY Buddy ! U r Doing Something Wrong ");
            e.printStackTrace();
        }
    }
}


