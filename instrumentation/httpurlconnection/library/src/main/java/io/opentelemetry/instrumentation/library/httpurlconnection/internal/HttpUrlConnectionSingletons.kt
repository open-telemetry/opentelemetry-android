/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection.internal

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientExperimentalMetrics
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientPeerServiceAttributesExtractor
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpExperimentalAttributesExtractor
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientMetrics
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanStatusExtractor
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlInstrumentation
import java.net.URLConnection

internal object HttpUrlConnectionSingletons {
    private const val INSTRUMENTATION_NAME = "io.opentelemetry.android.http-url-connection"

    private lateinit var instrumenter: Instrumenter<URLConnection, Int>
    private lateinit var openTelemetryInstance: OpenTelemetry

    fun configure(
        instrumentation: HttpUrlInstrumentation,
        openTelemetry: OpenTelemetry,
    ) {
        val httpAttributesGetter = HttpUrlHttpAttributesGetter()

        val httpSpanNameExtractorBuilder =
            HttpSpanNameExtractor
                .builder(httpAttributesGetter)
                .setKnownMethods(instrumentation.knownMethods)

        val httpClientAttributesExtractorBuilder =
            HttpClientAttributesExtractor
                .builder(httpAttributesGetter)
                .setCapturedRequestHeaders(
                    instrumentation.capturedRequestHeaders,
                ).setCapturedResponseHeaders(
                    instrumentation.capturedResponseHeaders,
                ).setKnownMethods(instrumentation.knownMethods)

        val httpClientPeerServiceAttributesExtractor =
            HttpClientPeerServiceAttributesExtractor.create(
                httpAttributesGetter,
                instrumentation.newPeerServiceResolver(),
            )

        openTelemetryInstance = openTelemetry

        val builder =
            Instrumenter
                .builder<URLConnection?, Int?>(
                    openTelemetryInstance,
                    INSTRUMENTATION_NAME,
                    httpSpanNameExtractorBuilder.build(),
                ).setSpanStatusExtractor(
                    HttpSpanStatusExtractor.create(httpAttributesGetter),
                ).addAttributesExtractor(httpClientAttributesExtractorBuilder.build())
                .addAttributesExtractor(httpClientPeerServiceAttributesExtractor)
                .addOperationMetrics(HttpClientMetrics.get())

        for (extractor in instrumentation.getAdditionalExtractors()) {
            builder.addAttributesExtractor(extractor)
        }

        if (instrumentation.emitExperimentalHttpClientMetrics()) {
            builder
                .addAttributesExtractor(
                    HttpExperimentalAttributesExtractor.create(httpAttributesGetter),
                ).addOperationMetrics(HttpClientExperimentalMetrics.get())
        }

        instrumenter = builder.buildClientInstrumenter(RequestPropertySetter)
    }

    @JvmStatic
    fun instrumenter(): Instrumenter<URLConnection, Int> = instrumenter

    @JvmStatic
    fun openTelemetryInstance(): OpenTelemetry = openTelemetryInstance
}
