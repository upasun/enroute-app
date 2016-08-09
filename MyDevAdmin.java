package com.example.android.enroute;

import android.app.admin.DeviceAdminReceiver;

/**
 * Created by VUSR on 6/22/2016.
 */
public class MyDevAdmin extends DeviceAdminReceiver {

    //created to allow and bind device admin permissions in manifest
    public MyDevAdmin(){
        super();
    }
}
