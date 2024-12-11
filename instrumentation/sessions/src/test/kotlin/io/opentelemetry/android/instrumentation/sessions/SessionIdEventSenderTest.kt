/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.sessions

import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.Session.DefaultSession
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.KeyValue
import io.opentelemetry.api.common.Value
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SessionIdEventSenderTest {
    private var underTest: SessionIdEventSender? = null

    @BeforeEach
    fun setup() {
        val loggerProvider = otelTesting.openTelemetry.logsBridge
        val eventLogger = SdkEventLoggerProvider.create(loggerProvider)["testLogging"]
        underTest = SessionIdEventSender(eventLogger)
    }

    @Test
    fun `should emit session start event`() {
        val newSession: Session = DefaultSession("123", 0)
        val oldSession: Session = DefaultSession("666", 0)
        underTest!!.onSessionStarted(newSession, oldSession)

        val logs = otelTesting.logRecords
        assertEquals(1, logs.size)
        val log = logs[0]

        // TODO: Use new event body assertions when available.
        assertThat(log)
            .hasAttributesSatisfyingExactly(OpenTelemetryAssertions.equalTo(AttributeKey.stringKey("event.name"), "session.start"))

        val body = log.bodyValue as Value<List<KeyValue>>?
        val kvBody = body!!.value
        assertThat(kvBody[0].key).isEqualTo("session.previous_id")
        assertThat(kvBody[0].value.asString()).isEqualTo("666")
        assertThat(kvBody[1].key).isEqualTo("session.id")
        assertThat(kvBody[1].value.asString()).isEqualTo("123")
    }

    @Test
    fun `should emit session end event`() {
        val session: Session = DefaultSession("123", 0)
        underTest!!.onSessionEnded(session)

        val logs = otelTesting.logRecords
        assertEquals(1, logs.size)
        val log = logs[0]

        // TODO: Use new event body assertions when available.
        assertThat(log)
            .hasAttributesSatisfyingExactly(
                OpenTelemetryAssertions.equalTo(
                    AttributeKey.stringKey("event.name"),
                    "session.end",
                ),
            )

        val body = log.bodyValue as Value<List<KeyValue>>?
        val kvBody = body!!.value
        assertThat(kvBody[0].key).isEqualTo("session.id")
        assertThat(kvBody[0].value.asString()).isEqualTo("123")
    }

    companion object {
        @RegisterExtension
        @JvmField
        var otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }
}
