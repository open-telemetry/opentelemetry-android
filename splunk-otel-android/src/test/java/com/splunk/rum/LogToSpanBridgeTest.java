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

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class LogToSpanBridgeTest {

    @RegisterExtension OpenTelemetryExtension testing = OpenTelemetryExtension.create();

    @Mock ReadWriteLogRecord logRecord;
    @Mock LogRecordData log;

    final LogToSpanBridge bridge = new LogToSpanBridge();

    @Test
    void misconfiguration() {
        bridge.onEmit(Context.root(), logRecord);

        assertThat(testing.getSpans()).isEmpty();
    }

    @Test
    void unnamedLog() {
        InstrumentationScopeInfo scope =
                InstrumentationScopeInfo.builder("test")
                        .setVersion("1.2.3")
                        .setSchemaUrl("http://schema")
                        .build();
        long epochNanos = 123_456_789_000_000L;
        when(log.getInstrumentationScopeInfo()).thenReturn(scope);
        when(log.getAttributes())
                .thenReturn(Attributes.builder().put("attr1", "12").put("attr2", "42").build());
        when(log.getEpochNanos()).thenReturn(epochNanos);
        when(log.getSeverity()).thenReturn(Severity.DEBUG);
        when(log.getSeverityText()).thenReturn("just testing");
        when(log.getBody()).thenReturn(Body.string("hasta la vista"));
        when(logRecord.toLogRecordData()).thenReturn(log);

        bridge.setTracerProvider(testing.getOpenTelemetry().getTracerProvider());
        bridge.onEmit(Context.root(), logRecord);

        List<SpanData> spans = testing.getSpans();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0))
                .hasInstrumentationScopeInfo(scope)
                .hasName("Log")
                .startsAt(epochNanos)
                .endsAt(epochNanos)
                .hasAttributes(
                        Attributes.builder()
                                .put("attr1", "12")
                                .put("attr2", "42")
                                .put(
                                        LogToSpanBridge.LOG_SEVERITY,
                                        Severity.DEBUG.getSeverityNumber())
                                .put(LogToSpanBridge.LOG_SEVERITY_TEXT, "just testing")
                                .put(LogToSpanBridge.LOG_BODY, "hasta la vista")
                                .build());
    }

    @Test
    void event() {
        long epochNanos = 123_456_789_000_000L;
        when(log.getInstrumentationScopeInfo()).thenReturn(InstrumentationScopeInfo.create("test"));
        when(log.getAttributes())
                .thenReturn(
                        Attributes.builder()
                                .put(SemanticAttributes.EVENT_DOMAIN, "androidApp")
                                .put(SemanticAttributes.EVENT_NAME, "buttonClick")
                                .put("attr", "value")
                                .build());
        when(log.getEpochNanos()).thenReturn(epochNanos);
        when(log.getSeverity()).thenReturn(Severity.UNDEFINED_SEVERITY_NUMBER);
        when(log.getBody()).thenReturn(Body.empty());
        when(logRecord.toLogRecordData()).thenReturn(log);

        bridge.setTracerProvider(testing.getOpenTelemetry().getTracerProvider());
        bridge.onEmit(Context.root(), logRecord);

        List<SpanData> spans = testing.getSpans();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0))
                .hasInstrumentationScopeInfo(InstrumentationScopeInfo.create("test"))
                .hasName("androidApp/buttonClick")
                .startsAt(epochNanos)
                .endsAt(epochNanos)
                .hasAttributes(
                        Attributes.builder()
                                .put(SemanticAttributes.EVENT_DOMAIN, "androidApp")
                                .put(SemanticAttributes.EVENT_NAME, "buttonClick")
                                .put("attr", "value")
                                .build());
    }

    @Test
    void customNamedLog() {
        long epochNanos = 123_456_789_000_000L;
        when(log.getInstrumentationScopeInfo()).thenReturn(InstrumentationScopeInfo.create("test"));
        when(log.getAttributes())
                .thenReturn(
                        Attributes.builder()
                                .put("attr1", "12")
                                .put("attr2", "42")
                                .put(LogToSpanBridge.OPERATION_NAME, "span name")
                                .build());
        when(log.getEpochNanos()).thenReturn(epochNanos);
        when(log.getSeverity()).thenReturn(Severity.INFO);
        when(log.getBody()).thenReturn(Body.string("message"));
        when(logRecord.toLogRecordData()).thenReturn(log);

        bridge.setTracerProvider(testing.getOpenTelemetry().getTracerProvider());
        bridge.onEmit(Context.root(), logRecord);

        List<SpanData> spans = testing.getSpans();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0))
                .hasInstrumentationScopeInfo(InstrumentationScopeInfo.create("test"))
                .hasName("span name")
                .startsAt(epochNanos)
                .endsAt(epochNanos)
                .hasAttributes(
                        Attributes.builder()
                                .put("attr1", "12")
                                .put("attr2", "42")
                                .put(LogToSpanBridge.OPERATION_NAME, "span name")
                                .put(
                                        LogToSpanBridge.LOG_SEVERITY,
                                        Severity.INFO.getSeverityNumber())
                                .put(LogToSpanBridge.LOG_BODY, "message")
                                .build());
    }
}
