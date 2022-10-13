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

import com.android.volley.Request;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper class containing the {@link Request} and {@linkplain Map additional headers}, as passed
 * to {@link com.android.volley.toolbox.BaseHttpStack}.
 */
public final class RequestWrapper {
    private final Request<?> request;
    private final Map<String, String> additionalHeaders;

    RequestWrapper(Request<?> request, Map<String, String> additionalHeaders) {
        this.request = request;
        this.additionalHeaders = new HashMap<>(additionalHeaders);
    }

    /** Returns the HTTP request that will be executed. */
    public Request<?> getRequest() {
        return request;
    }

    /** Returns additional headers that will be sent together with {@link Request#getHeaders()}. */
    public Map<String, String> getAdditionalHeaders() {
        return additionalHeaders;
    }
}
