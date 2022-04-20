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

import static org.junit.Assert.assertEquals;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SessionIdChangeTracerTest {
    @Rule public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();

    private SessionIdChangeListener underTest;

    @Before
    public void setup() {
        Tracer tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        underTest = new SessionIdChangeTracer(tracer);
    }

    @Test
    public void shouldEmitSessionIdChangeSpan() {
        underTest.onChange("123", "456");

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData span = spans.get(0);
        assertEquals("sessionId.change", span.getName());
        Attributes attributes = span.getAttributes();
        assertEquals(1, attributes.size());
        assertEquals("123", attributes.get(SessionIdChangeTracer.PREVIOUS_SESSION_ID_KEY));
        // splunk.rumSessionId attribute is set in the RumAttributeAppender class
    }
}
