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

import org.junit.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RumResponseAttributesExtractorTest {

    @Test
    public void spanDecoration() {
        ServerTimingHeaderParser headerParser = mock(ServerTimingHeaderParser.class);
        when(headerParser.parse("headerValue")).thenReturn(new String[]{"9499195c502eb217c448a68bfe0f967c", "fe16eca542cd5d86"});

        Request fakeRequest = mock(Request.class);
        Response response = new Response.Builder()
                .request(fakeRequest)
                .protocol(Protocol.HTTP_1_1)
                .message("hello")
                .code(200)
                .addHeader("Server-Timing", "headerValue")
                .addHeader("Content-Length", "101")
                .build();

        RumResponseAttributesExtractor attributesExtractor = new RumResponseAttributesExtractor(headerParser);
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onStart(attributesBuilder, fakeRequest);
        attributesExtractor.onEnd(attributesBuilder, fakeRequest, response);
        Attributes attributes = attributesBuilder.build();

        assertEquals("http", attributes.get(SplunkRum.COMPONENT_KEY));
        assertEquals("9499195c502eb217c448a68bfe0f967c", attributes.get(OkHttpRumInterceptor.LINK_TRACE_ID_KEY));
        assertEquals("fe16eca542cd5d86", attributes.get(OkHttpRumInterceptor.LINK_SPAN_ID_KEY));
        assertEquals(101L, (long) attributes.get(HTTP_RESPONSE_CONTENT_LENGTH));
    }

    @Test
    public void spanDecoration_noLinkingHeader() {
        ServerTimingHeaderParser headerParser = mock(ServerTimingHeaderParser.class);
        when(headerParser.parse(null)).thenReturn(new String[0]);

        Request fakeRequest = mock(Request.class);
        Response response = new Response.Builder()
                .request(fakeRequest)
                .protocol(Protocol.HTTP_1_1)
                .message("hello")
                .code(200)
                .build();

        RumResponseAttributesExtractor attributesExtractor = new RumResponseAttributesExtractor(headerParser);
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onEnd(attributesBuilder, fakeRequest, response);
        attributesExtractor.onStart(attributesBuilder, fakeRequest);
        Attributes attributes = attributesBuilder.build();

        assertEquals("http", attributes.get(SplunkRum.COMPONENT_KEY));
        assertNull(attributes.get(OkHttpRumInterceptor.LINK_TRACE_ID_KEY));
        assertNull(attributes.get(OkHttpRumInterceptor.LINK_SPAN_ID_KEY));
    }

    @Test
    public void spanDecoration_contentLength() {
        ServerTimingHeaderParser headerParser = mock(ServerTimingHeaderParser.class);
        when(headerParser.parse(null)).thenReturn(new String[0]);

        Request fakeRequest = mock(Request.class);
        Response response = new Response.Builder()
                .request(fakeRequest)
                .protocol(Protocol.HTTP_1_1)
                .message("hello")
                .addHeader("Content-Length", "101")
                .code(200)
                .build();

        RumResponseAttributesExtractor attributesExtractor = new RumResponseAttributesExtractor(headerParser);
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onEnd(attributesBuilder, fakeRequest, response);
        attributesExtractor.onStart(attributesBuilder, fakeRequest);
        Attributes attributes = attributesBuilder.build();

        assertEquals("http", attributes.get(SplunkRum.COMPONENT_KEY));
        assertEquals(101L, (long) attributes.get(HTTP_RESPONSE_CONTENT_LENGTH));
    }

}