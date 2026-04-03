/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import io.opentelemetry.android.common.RumConstants.Events.EVENT_SESSION_END
import io.opentelemetry.android.common.RumConstants.Events.EVENT_SESSION_START
import io.opentelemetry.android.session.Session
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_ID
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_PREVIOUS_ID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SessionIdEventSenderTest {
    private lateinit var logger: Logger

    private companion object {
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    @BeforeEach
    fun setup() {
        logger = otelTesting.openTelemetry.logsBridge.get("testLogger")
    }

    @AfterEach
    fun cleanup() {
        otelTesting.clearLogRecords()
    }

    @Test
    fun `starts first session`() {
        val sender = SessionIdEventSender(logger)
        val newSession = FakeSession("123", 12L)
        val previousSession = FakeSession("", -1)
        sender.onSessionStarted(newSession, previousSession)
        assertThat(otelTesting.logRecords).hasSize(1)
        val event = otelTesting.logRecords[0]
        assertThat(event.eventName).isEqualTo(EVENT_SESSION_START)
        assertThat(event.attributes.get(SESSION_ID)).isEqualTo(newSession.id)
        assertThat(event.attributes.get(SESSION_PREVIOUS_ID)).isNull()
    }

    @Test
    fun `starts new second session`() {
        val sender = SessionIdEventSender(logger)
        val previousSession = FakeSession("abc", 12L)
        val newSession = FakeSession("124", 13L)
        sender.onSessionStarted(newSession, previousSession)
        assertThat(otelTesting.logRecords).hasSize(1)
        val event = otelTesting.logRecords[0]
        assertThat(event.eventName).isEqualTo(EVENT_SESSION_START)
        assertThat(event.attributes.get(SESSION_ID)).isEqualTo(newSession.id)
        assertThat(event.attributes.get(SESSION_PREVIOUS_ID)).isEqualTo(previousSession.id)
    }

    @Test
    fun `ends a session`() {
        val sender = SessionIdEventSender(logger)
        val session = FakeSession("booper", 99L)
        sender.onSessionEnded(session)
        assertThat(otelTesting.logRecords).hasSize(1)
        val event = otelTesting.logRecords[0]
        assertThat(event.eventName).isEqualTo(EVENT_SESSION_END)
        assertThat(event.attributes.get(SESSION_ID)).isEqualTo(session.id)
        assertThat(event.attributes.get(SESSION_PREVIOUS_ID)).isNull()
    }

    @Test
    fun `ends an empty session`() {
        val sender = SessionIdEventSender(logger)
        val session = FakeSession("", -1)
        sender.onSessionEnded(session)
        assertThat(otelTesting.logRecords).isEmpty()
    }

    private class FakeSession(
        override val id: String,
        override val startTimestamp: Long,
    ) : Session
}
