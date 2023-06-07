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

package com.splunk.rum;

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
