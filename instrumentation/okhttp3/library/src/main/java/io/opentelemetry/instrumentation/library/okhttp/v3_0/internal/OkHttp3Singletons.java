/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientRequestResendCount;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.library.okhttp.v3_0.OkHttpInstrumentation;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.ConnectionErrorSpanInterceptor;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpAttributesGetter;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpClientInstrumenterBuilderFactory;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.TracingInterceptor;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OkHttp3Singletons {
    private static final Interceptor NOOP_INTERCEPTOR = chain -> chain.proceed(chain.request());
    public static Interceptor CONNECTION_ERROR_INTERCEPTOR = NOOP_INTERCEPTOR;
    public static Interceptor TRACING_INTERCEPTOR = NOOP_INTERCEPTOR;

    public static void configure(
            OkHttpInstrumentation instrumentation, OpenTelemetry openTelemetry) {
        DefaultHttpClientInstrumenterBuilder<Interceptor.Chain, Response> instrumenterBuilder =
                OkHttpClientInstrumenterBuilderFactory.create(openTelemetry)
                        .setCapturedRequestHeaders(instrumentation.getCapturedRequestHeaders())
                        .setCapturedResponseHeaders(instrumentation.getCapturedResponseHeaders())
                        .setKnownMethods(instrumentation.getKnownMethods())
                        // TODO: Do we really need to set the known methods on the span
                        // name
                        // extractor as well?
                        .setSpanNameExtractor(
                                x ->
                                        HttpSpanNameExtractor.builder(
                                                        OkHttpAttributesGetter.INSTANCE)
                                                .setKnownMethods(instrumentation.getKnownMethods())
                                                .build())
                        .addAttributesExtractor(
                                PeerServiceAttributesExtractor.create(
                                        OkHttpAttributesGetter.INSTANCE,
                                        instrumentation.newPeerServiceResolver()))
                        .setEmitExperimentalHttpClientTelemetry(
                                instrumentation.emitExperimentalHttpClientTelemetry());

        for (AttributesExtractor<Interceptor.Chain, Response> extractor :
                instrumentation.additionalExtractors) {
            instrumenterBuilder = instrumenterBuilder.addAttributesExtractor(extractor);
        }

        Instrumenter<Interceptor.Chain, Response> instrumenter = instrumenterBuilder.build();

        CONNECTION_ERROR_INTERCEPTOR = new ConnectionErrorSpanInterceptor(instrumenter);
        TRACING_INTERCEPTOR = new TracingInterceptor(instrumenter, openTelemetry.getPropagators());
    }

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

    private OkHttp3Singletons() {}
}
