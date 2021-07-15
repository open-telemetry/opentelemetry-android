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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WorkflowTimerTest {
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }


    @Test
    public void delegation() {
        Attributes eventAttributes = Attributes.of(stringKey("progress"), "halfway");

        Scope scope = mock(Scope.class);
        Span span = mock(Span.class);

        WorkflowTimer workflowTimer = new WorkflowTimer(span, scope);

        workflowTimer.addEvent("cheese");
        workflowTimer.addEvent("progress", eventAttributes);
        workflowTimer.setAttribute("flavor", "lemon");
        workflowTimer.setAttribute("things", 5);
        workflowTimer.setAttribute("fraction", 1.2345);
        workflowTimer.setAttribute("delicious", true);

        workflowTimer.end();

        verify(span).addEvent("cheese");
        verify(span).addEvent("progress", eventAttributes);
        verify(span).setAttribute("flavor", "lemon");
        verify(span).setAttribute("things", 5);
        verify(span).setAttribute("fraction", 1.2345);
        verify(span).setAttribute("delicious", true);

        InOrder inOrder = Mockito.inOrder(scope, span);
        inOrder.verify(scope).close();
        inOrder.verify(span).end();
    }

    @Test
    public void createAndClose() {
        try (WorkflowTimer workflowTimer = WorkflowTimer.create(tracer, "workflow")) {
            Span inner = tracer.spanBuilder("foo").startSpan();
            try (Scope scope = inner.makeCurrent()) {
                //do nothing
            } finally {
                inner.end();
            }
        }

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());
        assertEquals(spans.get(0).getParentSpanId(), spans.get(1).getSpanId());
        assertEquals(spans.get(0).getName(), "foo");
        assertEquals(spans.get(1).getName(), "workflow");
    }

    @Test
    public void closeAndEnd() {
        try (WorkflowTimer workflowTimer = WorkflowTimer.create(tracer, "workflow")) {
            //user shouldn't do this, but let's make sure this doesn't break anything too badly
            workflowTimer.end();
            workflowTimer.end();
            workflowTimer.end();
            workflowTimer.end();
        }

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertEquals(spans.get(0).getName(), "workflow");
    }
}