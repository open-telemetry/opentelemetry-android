/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.android.session.Session;
import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.incubator.events.EventLogger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SessionIdEventSenderTest {
    @RegisterExtension
    static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    private SessionIdEventSender underTest;

    @BeforeEach
    void setup() {
        LoggerProvider loggerProvider = otelTesting.getOpenTelemetry().getLogsBridge();
        EventLogger eventLogger = SdkEventLoggerProvider.create(loggerProvider).get("testLogging");
        underTest = new SessionIdEventSender(eventLogger);
    }

    @Test
    void shouldEmitSessionStartEvent() {
        Session newSession = new Session.DefaultSession("123", 0);
        Session oldSession = new Session.DefaultSession("666", 0);
        underTest.onSessionStarted(newSession, oldSession);

        List<LogRecordData> logs = otelTesting.getLogRecords();
        assertEquals(1, logs.size());
        LogRecordData log = logs.get(0);
        // TODO: Use new event body assertions when available.
        assertThat(log)
                .hasAttributesSatisfyingExactly(equalTo(stringKey("event.name"), "session.start"));

        Value<List<KeyValue>> body = (Value<List<KeyValue>>) log.getBodyValue();
        List<KeyValue> kvBody = body.getValue();
        assertThat(kvBody.get(0).getKey()).isEqualTo("session.previous_id");
        assertThat(kvBody.get(0).getValue().asString()).isEqualTo("666");
        assertThat(kvBody.get(1).getKey()).isEqualTo("session.id");
        assertThat(kvBody.get(1).getValue().asString()).isEqualTo("123");
    }

    @Test
    void shouldEmitSessionEndEvent() {
        Session session = new Session.DefaultSession("123", 0);
        underTest.onSessionEnded(session);

        List<LogRecordData> logs = otelTesting.getLogRecords();
        assertEquals(1, logs.size());
        LogRecordData log = logs.get(0);
        // TODO: Use new event body assertions when available.
        assertThat(log)
                .hasAttributesSatisfyingExactly(equalTo(stringKey("event.name"), "session.end"));

        Value<List<KeyValue>> body = (Value<List<KeyValue>>) log.getBodyValue();
        List<KeyValue> kvBody = body.getValue();
        assertThat(kvBody.get(0).getKey()).isEqualTo("session.id");
        assertThat(kvBody.get(0).getValue().asString()).isEqualTo("123");
    }
}
