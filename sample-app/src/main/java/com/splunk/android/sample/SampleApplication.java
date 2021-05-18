package com.splunk.android.sample;

import android.app.Application;

import com.splunk.rum.Config;
import com.splunk.rum.SplunkRum;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Config config = SplunkRum.newConfigBuilder()
                .beaconUrl("http://fill.me.in")
                .rumAuthToken("authTokenGoesHere")
                .build();
        SplunkRum.initialize(config);
    }
}
