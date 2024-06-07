/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection.internal;

import io.opentelemetry.api.GlobalOpenTelemetry;
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

    private static final Instrumenter<URLConnection, Integer> INSTRUMENTER;
    private static final String INSTRUMENTATION_NAME = "io.opentelemetry.android.http-url-connection";

    static {
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

        InstrumenterBuilder<URLConnection, Integer> builder =
                Instrumenter.<URLConnection, Integer>builder(
                                GlobalOpenTelemetry.get(),
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

        INSTRUMENTER = builder.buildClientInstrumenter(RequestPropertySetter.INSTANCE);
    }

    public static Instrumenter<URLConnection, Integer> instrumenter() {
        return INSTRUMENTER;
    }

    private HttpUrlConnectionSingletons() {}
}
