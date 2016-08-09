package com.example.android.enroute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//helper class for executing simple http get requests
public class SimpleHttpGet extends Thread implements Runnable
{
    private String address = "";
    private String response = "";

    public SimpleHttpGet()
    {

    }
    //set address with this method
    public void setAddress(String set){
        address = set;
    }

    //get response with this method
    public String getResponse(){
        return response;
    }

    //execute request by calling this.start(); where "this" is an instance of class
    public void run()
    {
        String desiredUrl = address;
        URL url;
        BufferedReader reader = null;
        StringBuilder stringBuilder;

        try
        {
            // create the HttpURLConnection
            url = new URL(desiredUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // just want to do an HTTP GET here
            connection.setRequestMethod("GET");

            // uncomment this if you want to write output to this url
            //connection.setDoOutput(true);

            // give it 15 seconds to respond
            connection.setReadTimeout(15*1000);
            connection.connect();

            // read the output from the server
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            stringBuilder = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null)
            {
                stringBuilder.append(line + "\n");
            }
            response = stringBuilder.toString();
            connection.disconnect();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            // close the reader; this can throw an exception too, so
            // wrap it in another try/catch block.
            if(response==null){
                response = "Cannot complete get request on URL";
            }
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                }
            }
        }
    }
}
