package com.example.android.enroute;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by VUSR on 6/30/2016.
 */

//second activity used for displaying the control panel if user is an admin
public class ControlActivity extends AppCompatActivity {

    private EditText ssd, slri, sspi; //set shortest disp/location request interval/server post interval
    private TextView csd, clri, cspi; //current shortest disp/location request interval/server post interval
    private String csdText = "Smallest Displacement: ";
    private String clriText = "Location Request Interval: ";
    private String cspiText = "Server Post Interval: ";
    private String s = "";

    private Intent serviceIntent;
    //This activity will launch on button press
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        csdText = "Smallest Displacement: ";
        clriText = "Location Request Interval: ";
        cspiText = "Server Post Interval: ";

        serviceIntent = new Intent(this, MainService.class);

        setContentView(R.layout.activity_control);
        Log.w("Control Activity", "Started");
    }

    @Override

    //displays current tracking parameter values and allows editing of values
    protected void onResume() {
        super.onResume();
        csd = (TextView) findViewById(R.id.CSD);
        clri = (TextView) findViewById(R.id.CLRI);
        cspi = (TextView) findViewById(R.id.CSPI);

        ssd = (EditText) findViewById(R.id.SSD);
        slri = (EditText) findViewById(R.id.SLRI);
        sspi = (EditText) findViewById(R.id.SSPI);

        csdText = "Smallest Displacement: ";
        clriText = "Location Request Interval: ";
        cspiText = "Server Post Interval: ";

        csdText += Integer.toString(MainService.setShortDisp);
        clriText += Long.toString(MainService.setLocReqInt);
        cspiText += Long.toString(MainService.setSerPosInt);

        csd.setText(csdText);
        clri.setText(clriText);
        cspi.setText(cspiText);
    }

    public void startTrackingService(View view){
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startService(serviceIntent);
        }
    }
    public void stopTrackingService(View view){
        stopService(serviceIntent);
    }

    public void goToOutputs(View view){
        Intent mainActInt = new Intent(this, MainActivity.class);
        startActivity(mainActInt);
    }

    //the onClick for the set button
    public void setValues(View view){
        int ssdVal;
        long slriVal;
        long sspiVal;
        try {
            if(!(ssd.getText().toString().equals("")) && ssd.getText().toString() != null) {
                Log.w("Control Activity", ssd.getText().toString());
                s = ssd.getText().toString();
                ssdVal = Integer.parseInt(s);
                MainService.setShortDisp = ssdVal;
            }
            if(!(slri.getText().toString().equals("")) && slri.getText().toString() != null) {
                Log.w("Control Activity", slri.getText().toString());
                s = slri.getText().toString();
                slriVal = Long.parseLong(s);
                MainService.setLocReqInt = slriVal;
            }
            if(!(sspi.getText().toString().equals("")) && sspi.getText().toString() != null) {
                Log.w("Control Activity", sspi.getText().toString());
                s = sspi.getText().toString();
                sspiVal = Long.parseLong(s);
                MainService.setSerPosInt = sspiVal;
            }

            csdText = "Smallest Displacement: ";
            clriText = "Location Request Interval: ";
            cspiText = "Server Post Interval: ";

            csdText += Integer.toString(MainService.setShortDisp);
            clriText += Long.toString(MainService.setLocReqInt);
            cspiText += Long.toString(MainService.setSerPosInt);

            csd.setText(csdText);
            clri.setText(clriText);
            cspi.setText(cspiText);
        }
        catch(Exception e){
            e.printStackTrace();
            Log.w("Control Activity", "Please input integers only");
        }
    }

}
