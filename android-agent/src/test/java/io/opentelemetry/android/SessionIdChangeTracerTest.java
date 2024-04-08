/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.internal.tools.RumConstants.PREVIOUS_SESSION_ID_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SessionIdChangeTracerTest {
    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    private SessionIdChangeListener underTest;

    @BeforeEach
    void setup() {
        Tracer tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        underTest = new SessionIdChangeTracer(tracer);
    }

    @Test
    void shouldEmitSessionIdChangeSpan() {
        underTest.onChange("123", "456");

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData span = spans.get(0);
        assertEquals("sessionId.change", span.getName());
        Attributes attributes = span.getAttributes();
        assertEquals(1, attributes.size());
        assertEquals("123", attributes.get(PREVIOUS_SESSION_ID_KEY));
        // splunk.rumSessionId attribute is set in the RumAttributeAppender class
    }
}
