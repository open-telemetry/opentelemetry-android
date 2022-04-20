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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AppStartupTimerTest {
    @Rule public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void start_end() {
        AppStartupTimer appStartupTimer = new AppStartupTimer();
        Span startSpan = appStartupTimer.start(tracer);
        assertNotNull(startSpan);
        appStartupTimer.end();

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData spanData = spans.get(0);

        assertEquals("AppStart", spanData.getName());
        assertEquals(
                SplunkRum.COMPONENT_APPSTART,
                spanData.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertEquals("cold", spanData.getAttributes().get(SplunkRum.START_TYPE_KEY));
    }

    @Test
    public void multi_end() {
        AppStartupTimer appStartupTimer = new AppStartupTimer();
        appStartupTimer.start(tracer);
        appStartupTimer.end();
        appStartupTimer.end();

        assertEquals(1, otelTesting.getSpans().size());
    }

    @Test
    public void multi_start() {
        AppStartupTimer appStartupTimer = new AppStartupTimer();
        appStartupTimer.start(tracer);
        assertSame(appStartupTimer.start(tracer), appStartupTimer.start(tracer));

        appStartupTimer.end();
        assertEquals(1, otelTesting.getSpans().size());
    }
}
