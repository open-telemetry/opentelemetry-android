package com.splunk.android.sample;

import android.app.Application;

import com.splunk.rum.Config;
import com.splunk.rum.SplunkRum;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Config config = SplunkRum.newConfigBuilder()
                // note: for these values to be resolved, put them in your local.properties file as
                // rum.beacon.url and rum.auth.token
                .beaconUrl(getResources().getString(R.string.rum_beacon_url))
                .rumAuthToken(getResources().getString(R.string.rum_auth_token))
                .applicationName("Android Demo App")
                .debugEnabled(true)
                .build();
        SplunkRum.initialize(config, this);
    }
}
