/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesExtractor;
import java.util.ArrayList;
import java.util.List;

/** A builder for {@link VolleyTracing}. */
public final class VolleyTracingBuilder {

    private static final String INSTRUMENTATION_NAME = "com.splunk.android.volley";

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
        VolleyNetClientAttributesGetter netAttributesGetter =
                VolleyNetClientAttributesGetter.INSTANCE;
        SpanStatusExtractor<RequestWrapper, HttpResponse> spanStatusExtractor =
                HttpSpanStatusExtractor.create(httpAttributesGetter);
        SpanNameExtractor<RequestWrapper> spanNameExtractor =
                HttpSpanNameExtractor.create(httpAttributesGetter);

        Instrumenter<RequestWrapper, HttpResponse> instrumenter =
                Instrumenter.<RequestWrapper, HttpResponse>builder(
                                openTelemetry, INSTRUMENTATION_NAME, spanNameExtractor)
                        .setSpanStatusExtractor(spanStatusExtractor)
                        .addAttributesExtractor(httpClientAttributesExtractorBuilder.build())
                        .addAttributesExtractor(
                                NetClientAttributesExtractor.create(netAttributesGetter))
                        .addAttributesExtractor(
                                new VolleyResponseAttributesExtractor(
                                        new ServerTimingHeaderParser()))
                        .addAttributesExtractors(additionalExtractors)
                        .buildClientInstrumenter(ClientRequestHeaderSetter.INSTANCE);

        return new VolleyTracing(instrumenter);
    }
}
