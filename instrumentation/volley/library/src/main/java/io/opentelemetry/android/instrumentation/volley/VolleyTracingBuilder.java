/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanStatusExtractor;
import java.util.ArrayList;
import java.util.List;

/** A builder for {@link VolleyTracing}. */
public final class VolleyTracingBuilder {

    private static final String INSTRUMENTATION_NAME = "io.opentelemetry.volley";

    private final OpenTelemetry openTelemetry;
    private final List<AttributesExtractor<RequestWrapper, HttpResponse>> additionalExtractors =
            new ArrayList<>();
    private final HttpClientAttributesExtractorBuilder<RequestWrapper, HttpResponse>
            httpClientAttributesExtractorBuilder =
                    HttpClientAttributesExtractor.builder(
                            VolleyHttpClientAttributesGetter.INSTANCE);

    VolleyTracingBuilder(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    /**
     * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
     * items.
     */
    public VolleyTracingBuilder addAttributesExtractor(
            AttributesExtractor<RequestWrapper, HttpResponse> attributesExtractor) {
        additionalExtractors.add(attributesExtractor);
        return this;
    }

    /**
     * Configures the HTTP request headers that will be captured as span attributes.
     *
     * @param requestHeaders A list of HTTP header names.
     */
    public VolleyTracingBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
        this.httpClientAttributesExtractorBuilder.setCapturedRequestHeaders(requestHeaders);
        return this;
    }

    /**
     * Configures the HTTP response headers that will be captured as span attributes.
     *
     * @param responseHeaders A list of HTTP header names.
     */
    public VolleyTracingBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
        this.httpClientAttributesExtractorBuilder.setCapturedResponseHeaders(responseHeaders);
        return this;
    }

    /**
     * Returns a new {@link VolleyTracing} with the settings of this {@link VolleyTracingBuilder}.
     */
    public VolleyTracing build() {
        VolleyHttpClientAttributesGetter httpAttributesGetter =
                VolleyHttpClientAttributesGetter.INSTANCE;
        SpanStatusExtractor<RequestWrapper, HttpResponse> spanStatusExtractor =
                HttpSpanStatusExtractor.create(httpAttributesGetter);
        SpanNameExtractor<RequestWrapper> spanNameExtractor =
                HttpSpanNameExtractor.create(httpAttributesGetter);

        Instrumenter<RequestWrapper, HttpResponse> instrumenter =
                Instrumenter.<RequestWrapper, HttpResponse>builder(
                                openTelemetry, INSTRUMENTATION_NAME, spanNameExtractor)
                        .setSpanStatusExtractor(spanStatusExtractor)
                        .addAttributesExtractor(httpClientAttributesExtractorBuilder.build())
                        .addAttributesExtractor(new VolleyResponseAttributesExtractor())
                        .addAttributesExtractors(additionalExtractors)
                        .buildClientInstrumenter(ClientRequestHeaderSetter.INSTANCE);

        return new VolleyTracing(instrumenter);
    }
}
