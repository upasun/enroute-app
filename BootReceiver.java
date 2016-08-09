package com.example.android.enroute;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by VUSR on 6/23/2016.
 */
public class BootReceiver extends BroadcastReceiver {

    //This class receives the boot completed intent and launches the main activity on boot
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);
    }
}
