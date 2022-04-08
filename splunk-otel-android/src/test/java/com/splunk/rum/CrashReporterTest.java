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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

public class CrashReporterTest {
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void crashReportingSpan() {
        TestDelegateHandler existingHandler = new TestDelegateHandler();
        SdkTracerProvider sdkTracerProvider = mock(SdkTracerProvider.class);
        CrashReporter.CrashReportingExceptionHandler crashReporter = new CrashReporter.CrashReportingExceptionHandler(tracer, sdkTracerProvider, existingHandler);

        NullPointerException oopsie = new NullPointerException("oopsie");
        Thread crashThread = new Thread("badThread");

        crashReporter.uncaughtException(crashThread, oopsie);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());

        assertThat(spans.get(0))
                .hasName("NullPointerException")
                .hasAttributesSatisfyingExactly(
                        equalTo(SemanticAttributes.THREAD_ID, crashThread.getId()),
                        equalTo(SemanticAttributes.THREAD_NAME, "badThread"),
                        equalTo(SemanticAttributes.EXCEPTION_ESCAPED, true),
                        equalTo(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_CRASH))
                .hasException(oopsie)
                .hasStatus(StatusData.error());

        assertTrue(existingHandler.wasDelegatedTo.get());
        verify(sdkTracerProvider).forceFlush();
    }

    private static class TestDelegateHandler implements Thread.UncaughtExceptionHandler {
        final AtomicBoolean wasDelegatedTo = new AtomicBoolean(false);

        @Override
        public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
            wasDelegatedTo.set(true);
        }
    }
}