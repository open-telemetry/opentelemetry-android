/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection.internal;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientExperimentalMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientPeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpExperimentalAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractorBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlInstrumentationConfig;
import java.net.URLConnection;

public final class HttpUrlConnectionSingletons {

    private static final Instrumenter<URLConnection, Integer> INSTRUMENTER;
    private static final String INSTRUMENTATION_NAME = "io.opentelemetry.auto-http-url-connection";

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
