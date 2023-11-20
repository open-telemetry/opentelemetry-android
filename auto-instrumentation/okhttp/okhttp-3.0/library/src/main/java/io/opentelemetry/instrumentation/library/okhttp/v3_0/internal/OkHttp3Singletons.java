/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal;

import static java.util.Collections.singletonList;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientRequestResendCount;
import io.opentelemetry.instrumentation.api.instrumenter.net.PeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.library.okhttp.v3_0.OkHttpInstrumentationConfig;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.ConnectionErrorSpanInterceptor;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpAttributesGetter;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpInstrumenterFactory;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.TracingInterceptor;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OkHttp3Singletons {

    private static final Instrumenter<Request, Response> INSTRUMENTER =
            OkHttpInstrumenterFactory.create(
                    GlobalOpenTelemetry.get(),
                    builder ->
                            builder.setCapturedRequestHeaders(
                                            OkHttpInstrumentationConfig.getCapturedRequestHeaders())
                                    .setCapturedResponseHeaders(
                                            OkHttpInstrumentationConfig
                                                    .getCapturedResponseHeaders())
                                    .setKnownMethods(OkHttpInstrumentationConfig.getKnownMethods()),
                    spanNameExtractorConfigurer ->
                            spanNameExtractorConfigurer.setKnownMethods(
                                    OkHttpInstrumentationConfig.getKnownMethods()),
                    singletonList(
                            PeerServiceAttributesExtractor.create(
                                    OkHttpAttributesGetter.INSTANCE,
                                    OkHttpInstrumentationConfig.newPeerServiceResolver())),
                    OkHttpInstrumentationConfig.emitExperimentalHttpClientMetrics());

    public static final Interceptor CALLBACK_CONTEXT_INTERCEPTOR =
            chain -> {
                Request request = chain.request();
                Context context =
                        OkHttpCallbackAdviceHelper.tryRecoverPropagatedContextFromCallback(request);
                if (context != null) {
                    try (Scope ignored = context.makeCurrent()) {
                        return chain.proceed(request);
                    }
                }

                return chain.proceed(request);
            };

    public static final Interceptor RESEND_COUNT_CONTEXT_INTERCEPTOR =
            chain -> {
                try (Scope ignored =
                        HttpClientRequestResendCount.initialize(Context.current()).makeCurrent()) {
                    return chain.proceed(chain.request());
                }
            };

    public static final Interceptor CONNECTION_ERROR_INTERCEPTOR =
            new ConnectionErrorSpanInterceptor(INSTRUMENTER);

    public static final Interceptor TRACING_INTERCEPTOR =
            new TracingInterceptor(INSTRUMENTER, GlobalOpenTelemetry.getPropagators());

    private OkHttp3Singletons() {}
}
