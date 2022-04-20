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

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import android.app.Application;
import com.splunk.rum.Config;
import com.splunk.rum.SplunkRum;
import com.splunk.rum.StandardAttributes;
import io.opentelemetry.api.common.Attributes;
import java.time.Duration;
import java.util.regex.Pattern;

public class SampleApplication extends Application {

    private static final Pattern HTTP_URL_SENSITIVE_DATA_PATTERN =
            Pattern.compile("(user|pass)=\\w+");

    @Override
    public void onCreate() {
        super.onCreate();

        Config config =
                SplunkRum.newConfigBuilder()
                        // note: for these values to be resolved, put them in your local.properties
                        // file as
                        // rum.beacon.url and rum.access.token
                        .realm(getResources().getString(R.string.rum_realm))
                        .slowRenderingDetectionPollInterval(Duration.ofMillis(1000))
                        .rumAccessToken(getResources().getString(R.string.rum_access_token))
                        .applicationName("Android Demo App")
                        .debugEnabled(true)
                        .diskBufferingEnabled(true)
                        .deploymentEnvironment("demo")
                        .limitDiskUsageMegabytes(1)
                        .globalAttributes(
                                Attributes.builder()
                                        .put("vendor", "Splunk")
                                        .put(
                                                StandardAttributes.APP_VERSION,
                                                BuildConfig.VERSION_NAME)
                                        .build())
                        .filterSpans(
                                spanFilter ->
                                        spanFilter
                                                .removeSpanAttribute(stringKey("http.user_agent"))
                                                .rejectSpansByName(
                                                        spanName -> spanName.contains("ignored"))
                                                // sensitive data in the login http.url attribute
                                                // will be redacted before it hits the exporter
                                                .replaceSpanAttribute(
                                                        StandardAttributes.HTTP_URL,
                                                        value ->
                                                                HTTP_URL_SENSITIVE_DATA_PATTERN
                                                                        .matcher(value)
                                                                        .replaceAll(
                                                                                "$1=<redacted>")))
                        .build();
        SplunkRum.initialize(config, this);
    }
}
