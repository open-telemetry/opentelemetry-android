/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.android.sample;

import android.app.Application;

import com.splunk.rum.Config;
import com.splunk.rum.SplunkRum;

import io.opentelemetry.api.common.Attributes;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Config config = SplunkRum.newConfigBuilder()
                // note: for these values to be resolved, put them in your local.properties file as
                // rum.beacon.url and rum.access.token
                .realm(getResources().getString(R.string.rum_realm))
                .rumAccessToken(getResources().getString(R.string.rum_access_token))
                .applicationName("Android Demo App")
                .debugEnabled(true)
                .deploymentEnvironment("demo")
                .globalAttributes(Attributes.builder().put("vendor", "Splunk").build())
                .build();
        SplunkRum.initialize(config, this);
    }
}
