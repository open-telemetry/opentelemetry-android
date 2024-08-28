/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.common.RumConstants.PREVIOUS_SESSION_ID_KEY;
import static io.opentelemetry.android.common.RumConstants.SESSION_ID_KEY;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.android.session.Session;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.events.EventLogger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.internal.AnyValueBody;
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SessionIdEventSenderTest {
    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    private SessionIdEventSender underTest;

    @BeforeEach
    void setup() {

//        val loggerProvider = rum?.openTelemetry?.logsBridge
//        val eventLogger =
//                SdkEventLoggerProvider.create(loggerProvider).get(scopeName)
//        return eventLogger.builder(eventName)

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
        assertThat(log).hasAttributesSatisfying( attrs ->
                OpenTelemetryAssertions.assertThat(attrs)
                        .containsEntry("event.name", "session.start"));

        AnyValueBody body = (AnyValueBody) log.getBody();
        fail("TODO: Finish fixing this test");
//        assertEquals("session.start", body.asAnyValue().getField(SESSION_ID_KEY);
//        Attributes attributes = log.getAttributes();
//        assertEquals(1, attributes.size());
//        assertEquals("123", attributes.get(PREVIOUS_SESSION_ID_KEY));
        // splunk.rumSessionId attribute is set in the RumAttributeAppender class
    }

    @Test
    void shouldEmitSessionEndEvent() {
        Session session = new Session.DefaultSession("123", 0);
        underTest.onSessionEnded(session);
        fail("Finish making this test");
    }
}
