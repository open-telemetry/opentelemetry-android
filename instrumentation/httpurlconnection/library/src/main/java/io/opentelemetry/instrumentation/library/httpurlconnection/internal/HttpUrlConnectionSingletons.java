/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection.internal;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientExperimentalMetrics;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientPeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpExperimentalAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlInstrumentation;
import java.net.URLConnection;

public final class HttpUrlConnectionSingletons {

    private static Instrumenter<URLConnection, Integer> instrumenter;
    private static final String INSTRUMENTATION_NAME =
            "io.opentelemetry.android.http-url-connection";
    private static OpenTelemetry openTelemetryInstance;

    public static void configure(
            HttpUrlInstrumentation instrumentation, OpenTelemetry openTelemetry) {

        HttpUrlHttpAttributesGetter httpAttributesGetter = new HttpUrlHttpAttributesGetter();

        HttpSpanNameExtractorBuilder<URLConnection> httpSpanNameExtractorBuilder =
                HttpSpanNameExtractor.builder(httpAttributesGetter)
                        .setKnownMethods(instrumentation.getKnownMethods());

        HttpClientAttributesExtractorBuilder<URLConnection, Integer>
                httpClientAttributesExtractorBuilder =
                        HttpClientAttributesExtractor.builder(httpAttributesGetter)
                                .setCapturedRequestHeaders(
                                        instrumentation.getCapturedRequestHeaders())
                                .setCapturedResponseHeaders(
                                        instrumentation.getCapturedResponseHeaders())
                                .setKnownMethods(instrumentation.getKnownMethods());

        HttpClientPeerServiceAttributesExtractor<URLConnection, Integer>
                httpClientPeerServiceAttributesExtractor =
                        HttpClientPeerServiceAttributesExtractor.create(
                                httpAttributesGetter, instrumentation.newPeerServiceResolver());

        openTelemetryInstance = openTelemetry;

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

        for (AttributesExtractor<URLConnection, Integer> extractor :
                instrumentation.getAdditionalExtractors()) {
            builder.addAttributesExtractor(extractor);
        }

        if (instrumentation.emitExperimentalHttpClientMetrics()) {
            builder.addAttributesExtractor(
                            HttpExperimentalAttributesExtractor.create(httpAttributesGetter))
                    .addOperationMetrics(HttpClientExperimentalMetrics.get());
        }

        instrumenter = builder.buildClientInstrumenter(RequestPropertySetter.INSTANCE);
    }

    public static Instrumenter<URLConnection, Integer> instrumenter() {
        return instrumenter;
    }

    public static OpenTelemetry openTelemetryInstance() {
        return openTelemetryInstance;
    }

    private HttpUrlConnectionSingletons() {}
}
