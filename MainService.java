package com.example.android.enroute;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by VUSR on 6/22/2016.
 */                                                 //interfaces to use Google APIs
public class MainService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleApiClient mGoogleApiClient = null;

    private LocationRequest mLocationRequest = new LocationRequest();

    private Calendar curTimeCal, cal, updateTime = Calendar.getInstance(); //calendars for time

    private AlarmManager aM; //to manage our repeating alarm

    private PendingIntent recurringAlarm; //the recurring intent sent to trigger server post with new payload

    private PowerManager.WakeLock wakeLock;

    private static Location mLastLocation = null;

    private long eleven59, lastTime = 0, timeLapsed = 0;

    protected static String timeStamp, timeLapseStamp, curLat, curLon, tenantid = "", userid = "", deviceid = "12344", mobile = "99999";

    protected static int setShortDisp = 20; //shortest displacement

    protected static long setLocReqInt = 25000;  //location request interval
    protected static long setSerPosInt = 1000 * 60 * 3;  //server post interval

    protected static String jsonScript = "";

    protected static int onLocCgd = 0;

    protected static ArrayList<String> listViewStrings = new ArrayList<String>(); //ArrayList used to populate "Server Posts"

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w("Main Service","Main Service Started");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            //gets phone # and device id
            TelephonyManager tMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);//line not needed right now
            mobile = tMgr.getLine1Number();
            Log.w("Main Service","Mobile Number: " + mobile);
            deviceid = tMgr.getDeviceId();
            Log.w("Main Service","Device ID: " + deviceid);

            //below block gets tenant id and user id with instance of object from helper class SimpleHttpGet
            SimpleHttpGet myGet = new SimpleHttpGet();
            myGet.setAddress("/*REDACTED*/"+mobile);
            try {
                myGet.start();
                while(myGet.getResponse().equals("")){

                }
                try {
                    String sub = myGet.getResponse().substring(1, myGet.getResponse().length() - 1);
                    JSONObject jo = new JSONObject(sub);
                    userid = jo.get("id").toString();
                    tenantid = jo.get("tenantid").toString();
                    Log.w("Main Service", userid);
                    Log.w("Main Service", tenantid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        //aquires a partial wake lock to keep checking for location updates at all times
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        //wakeLock.acquire();

        //sets the latest time in day for time elapsed calculations
        setEleven59();

        //starts a new payload body entry
        reset();

        //initializes google api client
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

        }

        mLastLocation = null;

        mGoogleApiClient.connect();

        //Add AlarmManager object to periodically post data to server
        aM = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        Intent myAlarm = new Intent(getApplicationContext(), AlarmReceiver.class);
        //myAlarm.putExtra("project_id", project_id); //Put Extra if needed
        recurringAlarm = PendingIntent.getBroadcast(getApplicationContext(), 0, myAlarm, PendingIntent.FLAG_CANCEL_CURRENT);

        //updateTime.setWhatever(0);    //set time to start first occurence of alarm                //3 min test alarm, starting after 30 sec

        aM.setRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis() + 1000 * 30, setSerPosInt, recurringAlarm);//fires alarm in intervals to post data to server
        Log.w("Main Service","Alarms Set");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //ensures service is restarted if killed
        return START_STICKY;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //check if we have location permission
        Log.w("Main Service","Location Services Connected");
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        //checks for an initial location
        for(int i = 0; i < 5; i++) {
            if (mLastLocation == null) {
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
            }
            else{
                break;
            }
        }
        mLocationRequest.setPriority(mLocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(setShortDisp);
        mLocationRequest.setInterval(setLocReqInt);
        mLocationRequest.setFastestInterval(15000);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        //addEntry(mLastLocation);
    }

    @Override
    public void onDestroy() {
        // release the wakelock
        if(wakeLock.isHeld()) {
            wakeLock.release();
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        aM.cancel(recurringAlarm);
    }

    //adds new entries into payload body as string
    private void addEntry(Location loc){
        if (loc != null) {
            curLat = (String.valueOf(loc.getLatitude()));
            curLon = (String.valueOf(loc.getLongitude()));
        }
        curTimeCal = Calendar.getInstance();
        timeStamp = Long.toString((curTimeCal.getTimeInMillis()));
        if(lastTime==0){
            timeLapsed = 0;
        }
        else {
            timeLapsed = curTimeCal.getTimeInMillis() - lastTime;
        }
        if(timeLapsed < 0){
            timeLapsed = eleven59 - lastTime + curTimeCal.getTimeInMillis();
            setEleven59();
        }
        timeLapseStamp = Long.toString(timeLapsed);
        jsonScript = insert(jsonScript,"{\n\"latitude\":\""+curLat+"\",\n" +
                "\"longitude\":\""+curLon+"\",\n" +
                "\"timestamp\":\""+timeStamp+"\",\n" +
                "\"timeElapsed\":\""+timeLapseStamp+"\"\n},\n",jsonScript.length()-3);
        lastTime = curTimeCal.getTimeInMillis();
        Log.w("Main Service","Entry Added");
    }

    public static String insert(String script, String entry, int index) {
        String sBegin = script.substring(0,index);
        String sEnd = script.substring(index);
        return sBegin + entry + sEnd;
    }

    //calculates milisecond value of 11:59pm for time elapsed calculations
    private void setEleven59(){
        cal = Calendar.getInstance(); //current date and time
        cal.set(Calendar.HOUR_OF_DAY, 23); //set hour to last hour
        cal.set(Calendar.MINUTE, 59); //set minutes to last minute
        cal.set(Calendar.SECOND, 59); //set seconds to last second
        cal.set(Calendar.MILLISECOND, 999); //set milliseconds to last millisecond
        eleven59 = cal.getTimeInMillis();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    //starts new payload if script is empty, or adds entry if it isn't
    public void onLocationChanged(Location location) {
        Log.w("Main Service","OnLocationChanged Triggered");

        mLastLocation = location;
        addEntry(location);
        onLocCgd++;
    }

    public static void reset(){
        jsonScript = "{\n\"id\":\""+userid+"\"," +
                "\n\"tenantid\":\""+tenantid+"\"," +
                "\n\"deviceid\":\""+deviceid+"\"," +
                "\n\"mobile\":\""+mobile+"\"," +
                "\n\"checkins\":[\n]\n}";
        Log.w("Main Service","New Payload Started");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
