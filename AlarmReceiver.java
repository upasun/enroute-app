package com.example.android.enroute;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by VUSR on 6/23/2016.
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {

    //This wakeful broadcast receiver triggers every x minutes to start the intent service to post JSON data to the server
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w("AlarmReceiver","onReceive Started");
        Intent serviceIntent = new Intent(context, locationservice.class);
        startWakefulService(context, serviceIntent);
        //the line above makes the intent service behave like a wakeFul intent service
    }
}
