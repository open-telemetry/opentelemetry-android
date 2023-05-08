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

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import java.util.concurrent.TimeUnit;

final class LogToSpanBridge implements LogRecordProcessor {

    // can be used in Splunk code to set span names in instrumentations
    static final AttributeKey<String> OPERATION_NAME = stringKey("log.operation_name");

    static final AttributeKey<Long> LOG_SEVERITY = longKey("log.severity");
    static final AttributeKey<String> LOG_SEVERITY_TEXT = stringKey("log.severity_text");
    static final AttributeKey<String> LOG_BODY = stringKey("log.body");

    private volatile TracerProvider tracerProvider;

    void setTracerProvider(TracerProvider tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    public void onEmit(Context context, ReadWriteLogRecord logRecord) {
        TracerProvider tracerProvider = this.tracerProvider;
        if (tracerProvider == null) {
            // if this is null then we've messed up the RumInitializer implementation
            return;
        }

        LogRecordData log = logRecord.toLogRecordData();
        Tracer tracer = getTracer(tracerProvider, log.getInstrumentationScopeInfo());

        SpanBuilder spanBuilder = tracer.spanBuilder(getSpanName(log));
        setLogAttributes(spanBuilder, log);
        Span span =
                spanBuilder
                        .setStartTimestamp(log.getTimestampEpochNanos(), TimeUnit.NANOSECONDS)
                        .startSpan();
        span.end(log.getTimestampEpochNanos(), TimeUnit.NANOSECONDS);
    }

    private static Tracer getTracer(TracerProvider tracerProvider, InstrumentationScopeInfo scope) {
        TracerBuilder builder = tracerProvider.tracerBuilder(scope.getName());
        String version = scope.getVersion();
        if (version != null) {
            builder.setInstrumentationVersion(version);
        }
        String schemaUrl = scope.getSchemaUrl();
        if (schemaUrl != null) {
            builder.setSchemaUrl(schemaUrl);
        }
        return builder.build();
    }

    private static String getSpanName(LogRecordData log) {
        String operationName = log.getAttributes().get(OPERATION_NAME);
        if (operationName != null) {
            return operationName;
        }
        String eventDomain = log.getAttributes().get(SemanticAttributes.EVENT_DOMAIN);
        String eventName = log.getAttributes().get(SemanticAttributes.EVENT_NAME);
        if (eventDomain != null || eventName != null) {
            return (eventDomain == null ? "" : eventDomain + "/")
                    + (eventName == null ? "" : eventName);
        }
        return "Log";
    }

    private static void setLogAttributes(SpanBuilder spanBuilder, LogRecordData log) {
        spanBuilder.setAllAttributes(log.getAttributes());
        int severity = log.getSeverity().getSeverityNumber();
        if (severity != Severity.UNDEFINED_SEVERITY_NUMBER.getSeverityNumber()) {
            spanBuilder.setAttribute(LOG_SEVERITY, (long) severity);
        }
        String severityText = log.getSeverityText();
        if (severityText != null) {
            spanBuilder.setAttribute(LOG_SEVERITY_TEXT, severityText);
        }
        Body logBody = log.getBody();
        switch (logBody.getType()) {
            case STRING:
                spanBuilder.setAttribute(LOG_BODY, logBody.asString());
                break;

            case EMPTY:
            default:
                break;
        }
    }
}
