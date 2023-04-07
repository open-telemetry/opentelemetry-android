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

import static com.android.volley.Request.Method.GET;

import com.android.volley.Cache;
import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.StringRequest;

import com.splunk.rum.SplunkRum;
import com.splunk.rum.VolleyTracing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Very simple example of making an instrumented Volley http request. This class leverages the
 * Splunk Android Volley instrumentation.
 */
public class VolleyExample {

    private static final String URL = "https://pmrum.o11ystore.com?user=me&pass=secret123secret";
    private final SplunkRum splunkRum;

    public VolleyExample(SplunkRum splunkRum) {
        this.splunkRum = splunkRum;
    }

    public void doHttpRequest() {
        List<String> requestHeaders = Arrays.asList("User-Agent", "Accept");
        List<String> responseHeaders = Arrays.asList("Date", "Content-Length", "Content-Type");
        VolleyTracing volleyTracing =
                VolleyTracing.builder(splunkRum)
                        .setCapturedRequestHeaders(requestHeaders)
                        .setCapturedResponseHeaders(responseHeaders)
                        .build();

        HurlStack hurlStack = volleyTracing.newHurlStack();
        RequestQueue queue = buildRequestQueue(hurlStack);

        Request<?> request =
                new StringRequest(
                        GET,
                        URL,
                        response -> {
                            System.out.println("Got response: " + response.substring(0, 100));
                        },
                        error -> {}) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> extraHeaders = new HashMap<>();
                        extraHeaders.put("User-Agent", "MyVolleyExample");
                        extraHeaders.put("Accept", "text/html, application/awesome");
                        extraHeaders.put(
                                "X-Android-Instrumentation-Example", "splunk-otel-android");
                        return extraHeaders;
                    }
                };
        queue.add(request);
    }

    /**
     * Creates and returns a simple non-caching RequestQueue for demo purposes. Volley users are
     * expected to know how to create their own request queue (see
     * https://google.github.io/volley/requestqueue.html). The main demonstration point here is how
     * the network is created using the HurlStack obtained from VolleyTracing.
     *
     * @param hurlStack - A HurlStack obtained from VolleyTracing
     * @return a newly created and started RequestQueue. It is the responsibility of the caller to
     *     stop the queue when finished.
     */
    private static RequestQueue buildRequestQueue(HurlStack hurlStack) {
        Cache cache = new NoCache();
        Network network = new BasicNetwork(hurlStack);

        RequestQueue queue =
                new RequestQueue(
                        cache,
                        network,
                        1,
                        new ExecutorDelivery(Executors.newSingleThreadExecutor()));
        queue.start();
        return queue;
    }
}
