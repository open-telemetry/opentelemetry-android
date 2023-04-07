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

import static com.splunk.rum.SplunkRum.COMPONENT_KEY;
import static com.splunk.rum.SplunkRum.LINK_SPAN_ID_KEY;
import static com.splunk.rum.SplunkRum.LINK_TRACE_ID_KEY;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import org.junit.jupiter.api.Test;

class RumResponseAttributesExtractorTest {

    @Test
    void spanDecoration() {
        ServerTimingHeaderParser headerParser = mock(ServerTimingHeaderParser.class);
        when(headerParser.parse("headerValue"))
                .thenReturn(new String[] {"9499195c502eb217c448a68bfe0f967c", "fe16eca542cd5d86"});

        Request fakeRequest = mock(Request.class);
        Response response =
                new Response.Builder()
                        .request(fakeRequest)
                        .protocol(Protocol.HTTP_1_1)
                        .message("hello")
                        .code(200)
                        .addHeader("Server-Timing", "headerValue")
                        .build();

        RumResponseAttributesExtractor attributesExtractor =
                new RumResponseAttributesExtractor(headerParser);
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onStart(attributesBuilder, Context.root(), fakeRequest);
        attributesExtractor.onEnd(attributesBuilder, Context.root(), fakeRequest, response, null);
        Attributes attributes = attributesBuilder.build();

        assertThat(attributes)
                .containsOnly(
                        entry(COMPONENT_KEY, "http"),
                        entry(LINK_TRACE_ID_KEY, "9499195c502eb217c448a68bfe0f967c"),
                        entry(LINK_SPAN_ID_KEY, "fe16eca542cd5d86"));
    }

    @Test
    void spanDecoration_noLinkingHeader() {
        ServerTimingHeaderParser headerParser = mock(ServerTimingHeaderParser.class);
        when(headerParser.parse(null)).thenReturn(new String[0]);

        Request fakeRequest = mock(Request.class);
        Response response =
                new Response.Builder()
                        .request(fakeRequest)
                        .protocol(Protocol.HTTP_1_1)
                        .message("hello")
                        .code(200)
                        .build();

        RumResponseAttributesExtractor attributesExtractor =
                new RumResponseAttributesExtractor(headerParser);
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onEnd(attributesBuilder, Context.root(), fakeRequest, response, null);
        attributesExtractor.onStart(attributesBuilder, Context.root(), fakeRequest);
        Attributes attributes = attributesBuilder.build();

        assertThat(attributes).containsOnly(entry(COMPONENT_KEY, "http"));
    }
}
