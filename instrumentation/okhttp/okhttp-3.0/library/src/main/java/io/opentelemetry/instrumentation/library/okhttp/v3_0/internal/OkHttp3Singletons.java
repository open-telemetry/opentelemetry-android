/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal;

import io.opentelemetry.android.instrumentation.AndroidInstrumentationRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientRequestResendCount;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.library.okhttp.v3_0.OkHttpInstrumentation;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.ConnectionErrorSpanInterceptor;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpAttributesGetter;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpClientInstrumenterBuilderFactory;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.TracingInterceptor;
import java.util.function.Supplier;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OkHttp3Singletons {

    private static final Supplier<Instrumenter<Request, Response>> INSTRUMENTER =
            CachedSupplier.create(
                    () -> {
                        OkHttpInstrumentation instrumentation =
                                AndroidInstrumentationRegistry.get()
                                        .get(OkHttpInstrumentation.class);
                        if (instrumentation == null) {
                            throw new IllegalStateException("OkHttpInstrumentation not found.");
                        }
                        return OkHttpClientInstrumenterBuilderFactory.create(
                                        GlobalOpenTelemetry.get())
                                .setCapturedRequestHeaders(
                                        instrumentation.getCapturedRequestHeaders())
                                .setCapturedResponseHeaders(
                                        instrumentation.getCapturedResponseHeaders())
                                .setKnownMethods(instrumentation.getKnownMethods())
                                // TODO: Do we really need to set the known methods on the span
                                // name
                                // extractor as well?
                                .setSpanNameExtractor(
                                        x ->
                                                HttpSpanNameExtractor.builder(
                                                                OkHttpAttributesGetter.INSTANCE)
                                                        .setKnownMethods(
                                                                instrumentation.getKnownMethods())
                                                        .build())
                                .addAttributeExtractor(
                                        PeerServiceAttributesExtractor.create(
                                                OkHttpAttributesGetter.INSTANCE,
                                                instrumentation.newPeerServiceResolver()))
                                .setEmitExperimentalHttpClientMetrics(
                                        instrumentation.emitExperimentalHttpClientMetrics())
                                .build();
                    });

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
            new LazyInterceptor<>(
                    CachedSupplier.create(
                            () -> new ConnectionErrorSpanInterceptor(INSTRUMENTER.get())));

    public static final Interceptor TRACING_INTERCEPTOR =
            new LazyInterceptor<>(
                    CachedSupplier.create(
                            () ->
                                    new TracingInterceptor(
                                            INSTRUMENTER.get(),
                                            GlobalOpenTelemetry.getPropagators())));

    private OkHttp3Singletons() {}
}
