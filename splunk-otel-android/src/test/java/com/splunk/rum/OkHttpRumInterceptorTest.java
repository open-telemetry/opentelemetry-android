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

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OkHttpRumInterceptorTest {

    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void spanDecoration_error() throws IOException {
        Interceptor.Chain fakeChain = mock(Interceptor.Chain.class);
        Request fakeRequest = mock(Request.class);
        when(fakeChain.request()).thenReturn(fakeRequest);
        when(fakeChain.proceed(fakeRequest)).thenThrow(new IOException("failed to make a call"));

        OkHttpRumInterceptor interceptor = new OkHttpRumInterceptor(new TestTracingInterceptor(tracer));
        assertThrows(IOException.class, () -> interceptor.intercept(fakeChain));

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData spanData = spans.get(0);
        assertEquals("IOException", spanData.getAttributes().get(SemanticAttributes.EXCEPTION_TYPE));
        assertEquals("failed to make a call", spanData.getAttributes().get(SemanticAttributes.EXCEPTION_MESSAGE));

        //temporary attributes until the RUM UI/backend can be brought up to date with otel conventions.
        assertEquals("IOException", spanData.getAttributes().get(SplunkRum.ERROR_TYPE_KEY));
        assertEquals("failed to make a call", spanData.getAttributes().get(SplunkRum.ERROR_MESSAGE_KEY));
    }

    private static class TestTracingInterceptor implements Interceptor {
        private final Tracer tracer;

        public TestTracingInterceptor(Tracer tracer) {
            this.tracer = tracer;
        }

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Span httpSpan = tracer.spanBuilder("httpSpan").startSpan();
            try (Scope scope = httpSpan.makeCurrent()) {
                return chain.proceed(chain.request());
            } finally {
                httpSpan.end();
            }
        }
    }
}