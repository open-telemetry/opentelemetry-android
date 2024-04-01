/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.HurlStack;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import javax.net.ssl.SSLSocketFactory;

/** Entrypoint for tracing Volley clients. */
public final class VolleyTracing {

    /** Returns a new {@link VolleyTracing} configured with the given {@link OpenTelemetry}. */
    public static VolleyTracing create(OpenTelemetry openTelemetry) {
        return builder(openTelemetry).build();
    }

    /**
     * Returns a new {@link VolleyTracingBuilder} configured with the given {@link OpenTelemetry}.
     */
    public static VolleyTracingBuilder builder(OpenTelemetry openTelemetry) {
        return new VolleyTracingBuilder(openTelemetry);
    }

    private final Instrumenter<RequestWrapper, HttpResponse> instrumenter;

    VolleyTracing(Instrumenter<RequestWrapper, HttpResponse> instrumenter) {
        this.instrumenter = instrumenter;
    }

    /** Returns a new {@link HurlStack} capable of tracing requests. */
    public HurlStack newHurlStack() {
        return new TracingHurlStack(instrumenter);
    }

    /**
     * Returns a new {@link HurlStack} capable of tracing requests configured with given {@link
     * HurlStack.UrlRewriter}.
     */
    public HurlStack newHurlStack(HurlStack.UrlRewriter urlRewriter) {
        return new TracingHurlStack(instrumenter, urlRewriter);
    }

    /**
     * Returns a new {@link HurlStack} capable of tracing requests configured with given {@link
     * HurlStack.UrlRewriter} and {@link SSLSocketFactory}.
     */
    public HurlStack newHurlStack(
            HurlStack.UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
        return new TracingHurlStack(instrumenter, urlRewriter, sslSocketFactory);
    }
}
