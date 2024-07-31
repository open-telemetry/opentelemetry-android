/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection.internal;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientExperimentalMetrics;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientPeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpExperimentalAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlInstrumentationConfig;
import java.net.URLConnection;

public final class HttpUrlConnectionSingletons {

    private static volatile Instrumenter<URLConnection, Integer> instrumenter;
    private static final String INSTRUMENTATION_NAME =
            "io.opentelemetry.android.http-url-connection";
    private static final Object lock = new Object();
    private static OpenTelemetry openTelemetryInstance;

    public static Instrumenter<URLConnection, Integer> createInstrumenter() {

        HttpUrlHttpAttributesGetter httpAttributesGetter = new HttpUrlHttpAttributesGetter();

        HttpSpanNameExtractorBuilder<URLConnection> httpSpanNameExtractorBuilder =
                HttpSpanNameExtractor.builder(httpAttributesGetter)
                        .setKnownMethods(HttpUrlInstrumentationConfig.getKnownMethods());

        HttpClientAttributesExtractorBuilder<URLConnection, Integer>
                httpClientAttributesExtractorBuilder =
                        HttpClientAttributesExtractor.builder(httpAttributesGetter)
                                .setCapturedRequestHeaders(
                                        HttpUrlInstrumentationConfig.getCapturedRequestHeaders())
                                .setCapturedResponseHeaders(
                                        HttpUrlInstrumentationConfig.getCapturedResponseHeaders())
                                .setKnownMethods(HttpUrlInstrumentationConfig.getKnownMethods());

        HttpClientPeerServiceAttributesExtractor<URLConnection, Integer>
                httpClientPeerServiceAttributesExtractor =
                        HttpClientPeerServiceAttributesExtractor.create(
                                httpAttributesGetter,
                                HttpUrlInstrumentationConfig.newPeerServiceResolver());

        openTelemetryInstance = GlobalOpenTelemetry.get();

        InstrumenterBuilder<URLConnection, Integer> builder =
                Instrumenter.<URLConnection, Integer>builder(
                                openTelemetryInstance,
                                INSTRUMENTATION_NAME,
                                httpSpanNameExtractorBuilder.build())
                        .setSpanStatusExtractor(
                                HttpSpanStatusExtractor.create(httpAttributesGetter))
                        .addAttributesExtractor(httpClientAttributesExtractorBuilder.build())
                        .addAttributesExtractor(httpClientPeerServiceAttributesExtractor)
                        .addOperationMetrics(HttpClientMetrics.get());

        if (HttpUrlInstrumentationConfig.emitExperimentalHttpClientMetrics()) {
            builder.addAttributesExtractor(
                            HttpExperimentalAttributesExtractor.create(httpAttributesGetter))
                    .addOperationMetrics(HttpClientExperimentalMetrics.get());
        }

        return builder.buildClientInstrumenter(RequestPropertySetter.INSTANCE);
    }

    public static Instrumenter<URLConnection, Integer> instrumenter() {
        if (instrumenter == null) {
            synchronized (lock) {
                if (instrumenter == null) {
                    instrumenter = createInstrumenter();
                }
            }
        }
        return instrumenter;
    }

    public static OpenTelemetry openTelemetryInstance() {
        return openTelemetryInstance;
    }

    private HttpUrlConnectionSingletons() {}
}
