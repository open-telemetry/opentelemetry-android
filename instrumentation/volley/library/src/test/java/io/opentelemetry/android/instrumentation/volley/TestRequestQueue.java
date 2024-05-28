/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import com.android.volley.Cache;
import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import java.util.concurrent.Executors;

class TestRequestQueue {

    private final RequestQueue queue;

    public static TestRequestQueue create(HurlStack hurlStack) {
        return new TestRequestQueue(hurlStack);
    }

    private TestRequestQueue(HurlStack hurlStack) {
        Cache cache = new NoCache();
        Network network = new BasicNetwork(hurlStack);

        queue =
                new RequestQueue(
                        cache,
                        network,
                        1,
                        new ExecutorDelivery(Executors.newSingleThreadExecutor()));
        queue.start();
    }

    public <T> void addToQueue(Request<T> req) {
        queue.add(req);
    }
}
