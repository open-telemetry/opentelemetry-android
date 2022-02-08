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

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.CapturedHttpHeaders;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;

/**
 * A builder for {@link VolleyTracing}.
 */
public final class VolleyTracingBuilder {

    private static final String INSTRUMENTATION_NAME = "com.splunk.android.volley";

    private final OpenTelemetry openTelemetry;
    private final List<AttributesExtractor<RequestWrapper, HttpResponse>> additionalExtractors =
            new ArrayList<>();
    private CapturedHttpHeaders capturedHttpHeaders = CapturedHttpHeaders.server(Config.get());

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
     * Configure the instrumentation to capture chosen HTTP request and response headers as span
     * attributes.
     *
     * @param capturedHttpHeaders An instance of {@link CapturedHttpHeaders} containing the configured
     *                            HTTP request and response names.
     */
    public VolleyTracingBuilder captureHttpHeaders(CapturedHttpHeaders capturedHttpHeaders) {
        this.capturedHttpHeaders = capturedHttpHeaders;
        return this;
    }

    /**
     * Returns a new {@link VolleyTracing} with the settings of this {@link VolleyTracingBuilder}.
     */
    public VolleyTracing build() {
        VolleyHttpClientAttributesExtractor httpAttributesExtractor =
                new VolleyHttpClientAttributesExtractor(capturedHttpHeaders);
        VolleyNetClientAttributesExtractor netAttributesExtractor =
                new VolleyNetClientAttributesExtractor();
        SpanStatusExtractor<RequestWrapper, HttpResponse> spanStatusExtractor =
                HttpSpanStatusExtractor.create(httpAttributesExtractor);
        SpanNameExtractor<RequestWrapper> spanNameExtractor =
                HttpSpanNameExtractor.create(httpAttributesExtractor);

        Instrumenter<RequestWrapper, HttpResponse> instrumenter =
                Instrumenter.<RequestWrapper, HttpResponse>builder(
                        openTelemetry, INSTRUMENTATION_NAME, spanNameExtractor)
                        .setSpanStatusExtractor(spanStatusExtractor)
                        .addAttributesExtractor(httpAttributesExtractor)
                        .addAttributesExtractor(netAttributesExtractor)
                        .addAttributesExtractor(new VolleyResponseAttributesExtractor(new ServerTimingHeaderParser()))
                        .addAttributesExtractors(additionalExtractors)
                        .newClientInstrumenter(ClientRequestHeaderSetter.INSTANCE);

        return new VolleyTracing(instrumenter);
    }

}
