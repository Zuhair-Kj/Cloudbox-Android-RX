package com.example.cbrxdemo;

import android.app.Application;

import com.duriana.cloudbox.CloudBox;

/**
 * Created by Zuhair on 11/1/16.
 */

public class MyApplication extends Application {

    public static CloudBox cloudBoxObject;

    @Override
    public void onCreate() {
        super.onCreate();
        cloudBoxObject = CloudBox.getInstance(this, "https://cloudbox.duriana.com").setMetaPrefix("/GBCloudBoxResourcesMeta/").setLogEnabled(true);
    }
}
