package com.example.android.enroute;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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


/**
 * Created by VUSR on 6/22/2016.
 */
public class locationservice extends IntentService{

    //This intent service handles posting JSON data to server

    private static final String address = "http://ec2-54-179-148-5.ap-southeast-1.compute.amazonaws.com:6001/api/v1.0/tracker";
    private static int changes = 0;

    public locationservice(){
        super("locationservice");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public locationservice(String name) {
        super(name);
    }



    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Add code here
        String response = "";
        String msg = "";
        Log.w("Intent Service","Started");


        //This ensures we are connected to the internet
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        //if we are connected and entries have been added to the payload
        if (networkInfo != null && networkInfo.isConnected() && (MainService.onLocCgd-changes != 0)) {

            //Block below finalizes payload body
            if(MainService.jsonScript.substring(MainService.jsonScript.length() - 5,MainService.jsonScript.length() - 4).equals(",")){
                msg = MainService.jsonScript.substring(0, MainService.jsonScript.length() - 5) +
                        MainService.jsonScript.substring(MainService.jsonScript.length() - 4);
            }
            else{
                msg = MainService.jsonScript;
            }

            URL url = null;
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
                writer.write(msg);
                writer.flush();
                writer.close();
                os.close();

                Log.w("Intent Service", "Current Payload:\n"+ msg);

                int responseCode=conn.getResponseCode();

                Log.w("Intent Service", "Response Code: "+Integer.toString(responseCode));

                //gets response
                //if the response is good, it resets the payload
                //if the post doesnt go through, the current payload is kept and added on to.
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line=br.readLine()) != null) {
                        response+=line;
                    }
                    MainService.reset();
                    MainService.listViewStrings.add("Payload:\n" + msg + /*"\nLocation Changes Triggered: " + Integer.toString(MainService.onLocCgd-changes) +
                            "\nTotal Triggers Since Start: " + Integer.toString(MainService.onLocCgd) +*/ "\nResponse: " + response);
                }
                else {
                    response="Server POST Error";
                    MainService.listViewStrings.add("Payload:\n" + msg + /*"\nLocation Changes Triggered: " + Integer.toString(MainService.onLocCgd-changes) +
                            "\nTotal Triggers Since Start: " + Integer.toString(MainService.onLocCgd) +*/ "\nResponse: " + response);

                }
                changes = MainService.onLocCgd;

                Log.w("Intent Service", response);

                //disconnects http connection
                conn.disconnect();
            }
            catch (IOException e) {
                e.printStackTrace();
                Log.w("Intent Service","Cannot Post Using Current HTTP Access");
                MainService.listViewStrings.add("\nCannot Post Using Current HTTP Access\n");
            }

            // fetch data
        } else {
            // display error
            if(MainService.onLocCgd-changes == 0){
                Log.w("Intent Service", "No new payload, POST cancelled");
                MainService.listViewStrings.add("\nNo new payload, POST cancelled\n");
            }
            else {
                Log.w("Intent Service", "POST ERROR, not connected to internet");
                MainService.listViewStrings.add("\nPOST ERROR, not connected to internet\n");
            }
        }

        Log.w("Intent Service","Finished");
        //finishes wakeful intent
        AlarmReceiver.completeWakefulIntent(intent);
    }

}
