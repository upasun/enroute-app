package com.example.android.enroute;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

//similar to the simple http get, but with an added payload parameter which must be a JSON formatted string
public class SimpleHttpPost extends Thread implements Runnable
{
    private String address = "";
    private String response = "";
    private String jsonPayload = "";

    public SimpleHttpPost()
    {

    }

    public void setAddress(String set){
        address = set;
    }

    public void setPayload(String set){
        jsonPayload = set;
    }

    public String getResponse(){
        return response;
    }

    public void run()
    {
        URL url = null;
        BufferedReader reader = null;

        try {
            url = new URL(address);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            //opens http connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(30000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            //sets property to JSON
            conn.addRequestProperty("Accept", "application/json");
            conn.addRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();

            //sets buffered writer
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));

            //writes payload
            writer.write(jsonPayload);
            writer.flush();
            writer.close();
            os.close();

            int responseCode=conn.getResponseCode();

            //gets response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                response="Server POST Error";
            }

            //disconnects http connection
            conn.disconnect();
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.w("Intent Service","Cannot Post Using Current HTTP Access");
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