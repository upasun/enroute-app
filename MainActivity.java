package com.example.android.enroute;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

    private static final int REQUEST_LOCATION = 0;

    private static final int REQUEST_PHONE_STATE = 2;

    private static String adminURL = "http://ec2-54-179-148-5.ap-southeast-1.compute.amazonaws.com:6001/api/v1.0/users?mobile=";

    private static ArrayList<String> permissions = new ArrayList<String>();

    private static boolean devStatusChecked = false;

    private static boolean isAdmin;

    protected static String devID, devNum, getResponse, uid, tid, rid;

    private Intent serviceIntent;

    DevicePolicyManager mDPM;
    ComponentName mDRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceIntent = new Intent(this, MainService.class);

        setContentView(R.layout.activity_main);
        Log.w("Main Activity", "Started");
        //May need to check for google api connectivity in main activity because
        //the permissions can't be requested in service; however, there have been no issues so far
    }

    //checks for permissions
    private void checkPermissions(){
        mDRV = new ComponentName(this, MyDevAdmin.class);
        Log.w("Main Activity","Permissions Requested");
        //checkDevAd();         //uncomment this line to make app a device admin WARNING! it will be a hassle to uninstall or stop if done
        checkPstate();
        checkLoc();

        //if the user is not an admin, and permissions have been granted, main service is started
        if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
            if(!devStatusChecked) {
                checkIfAdmin();
            }
            addControlsButton();
            if(!isAdmin) {
                startMainService();
            }
        }
    }

    private void checkLoc() {
        //location access permissions
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Main Activity","Location Services Currently Disabled");
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            String[] set = makeSet(permissions);
            ActivityCompat.requestPermissions(this, set, REQUEST_LOCATION);

        }
        else{
            Log.w("Main Activity","Location Services Enabled");
        }
    }

    //helper method for requesting permissions
    private String[] makeSet(ArrayList<String> sList){
        int j = sList.size();
        String[] set = new String[j];
        for(int x = 0; x < j; x++){
            set[x] = sList.get(x);
        }
        return set;
    }

    private void checkPstate() {
        //phone state access permissions
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Main Activity","Phone State Access Currently Disabled");
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        else{
            Log.w("Main Activity","Phone State Access Currently Enabled");
        }
    }

    private void checkDevAd() {
        //device admin permissions
        if(mDPM.isAdminActive(mDRV) == false){
            Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDRV);
            startActivityForResult(adminIntent, REQUEST_CODE_ENABLE_ADMIN);
        }
    }

    //starts the main service used for tracking location changes and time elapsed
    private void startMainService(){
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startService(serviceIntent);
        }

    }

    @Override
    protected void onStart(){
        super.onStart();
        checkPermissions();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        //checkPermissions();
    }

    @Override
    protected void onResume(){
        super.onResume();
        String[] sSet = makeSet(MainService.listViewStrings);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, sSet);

        ListView listView = (ListView) findViewById(R.id.payloadLV);
        listView.setAdapter(adapter);

        //checkPermissions();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override  //this method is auto-called after the user responds to permission requests
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.w("Main Activity", "onRequestPermissionResult Triggered");
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.w("Main Activity", "onRequestPermissionResult Triggered 1");
                } else {
                    Log.w("Main Activity", "onRequestPermissionResult Triggered 2");
                }
                if (grantResults.length > 0
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.w("Main Activity", "onRequestPermissionResult Triggered 3");
                    checkIfAdmin();
                    addControlsButton();
                    if(!isAdmin) {
                        startMainService();
                    }
                } else {
                    Log.w("Main Activity", "onRequestPermissionResult Triggered 4");
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    //method uses helper SimpleHttpGet obj to execute get request to check if number is that of an admins
    private void checkIfAdmin() {
        Log.w("Main Activity", "Checking if Admin");

        adminURL = "http://ec2-54-179-148-5.ap-southeast-1.compute.amazonaws.com:6001/api/v1.0/users?mobile=";


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            //gets phone # and device id
            TelephonyManager tMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            devNum = tMgr.getLine1Number();
            //Log.w("Main Service","Mobile Number: " + devID);
            devID = tMgr.getDeviceId();
            //Log.w("Main Service","Device ID: " + devNum);
        }
        if(devNum != null){
            adminURL+=devNum;
            try
            {
                Log.w("Main Activity", "url: " + adminURL);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            SimpleHttpGet myGet = new SimpleHttpGet();
            myGet.setAddress(adminURL);
            try {
                myGet.start();
                //may not need the loop because of synchronization, will need to do more research on subject
                while(myGet.getResponse().equals("")){

                }
                getResponse = myGet.getResponse();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //converts response to json object to get required fields
            if(getResponse != null) {
                String num;
                if(getResponse.length() > 5) {
                    try {
                        String sub = getResponse.substring(1, getResponse.length() - 1);
                        JSONObject jo = new JSONObject(sub);
                        uid = jo.get("id").toString();
                        tid = jo.get("tenantid").toString();
                        rid = jo.get("roleid").toString();
                        Log.w("Main Activity", uid);
                        Log.w("Main Activity", tid);
                        Log.w("Main Activity", rid);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                Log.w("Main Activity", getResponse);
                num = rid;
                }
                else{
                    num = "ERROR - no Response, Response: " + getResponse;
                }

                Log.w("Main Activity", num);
                if (num.equals("2")) {
                    isAdmin = true;
                }
                else{
                    isAdmin = false;
                }
            }
        }
        else {
            getResponse = "Phone permissions not granted";
            Log.w("Main Activity", getResponse);
        }
        devStatusChecked = true;
        Log.w("Main Activity", "Server Admin Status Checked");
    }

    //if the phone is an admin phone, the controls button is added to the bottom of the Server Posts screen
    private void addControlsButton(){
        if(isAdmin) {
            Log.w("Main Activity", "Registered admin, adding controls button");
            Button controls = new Button(this);
            controls.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    Intent controlActIntent = new Intent(getApplicationContext(),ControlActivity.class);
                    startActivity(controlActIntent);
                }
            });
            controls.setText("CONTROLS");

            RelativeLayout ll = (RelativeLayout) findViewById(R.id.relLay);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            ll.addView(controls, lp);
        }
    }
}

