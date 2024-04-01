/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

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
