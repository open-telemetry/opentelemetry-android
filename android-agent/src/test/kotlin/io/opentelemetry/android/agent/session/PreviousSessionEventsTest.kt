/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.sessions.SessionInstrumentation
import io.opentelemetry.android.session.Session
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.time.TestClock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(Incubating::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [23, 36])
class PreviousSessionEventsTest {
    @Test
    fun `startup exports only a linked start and reset links within the new process`() {
        val previous = SessionImpl("previous-launch", 12)
        val storage =
            object : SessionStorage {
                private var stored: Session = previous

                override fun get(): Session = stored

                override fun getPreviousSession(): Session = stored

                override fun save(newSession: Session) {
                    stored = newSession
                }
            }
        val clock = TestClock.create()
        val manager = SessionManager(clock, storage, SessionIdTimeoutHandler(clock, 15.minutes), maxSessionLifetime = 4.hours)
        val exporter = InMemoryLogRecordExporter.create()
        val logger = SdkLoggerProvider.builder().addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter)).build()
        OpenTelemetrySdk.builder().setLoggerProvider(logger).build().use { sdk ->
            val rum = mockk<OpenTelemetryRum>()
            every { rum.openTelemetry } returns sdk
            every { rum.sessionProvider } returns manager
            SessionInstrumentation().install(RuntimeEnvironment.getApplication(), rum)
            val first = manager.getSessionId()
            assertThat(exporter.finishedLogRecordItems.map { it.eventName }).containsExactly("session.start")
            assertThat(
                exporter.finishedLogRecordItems
                    .single()
                    .attributes
                    .get(stringKey("session.previous_id")),
            ).isEqualTo(previous.id)
            assertThat(manager.resetSession()).isTrue()
            val second = manager.getSessionId()
            val records = exporter.finishedLogRecordItems
            assertThat(records.map { it.eventName }).containsExactly("session.start", "session.end", "session.start")
            assertThat(records.map { it.attributes.get(stringKey("session.id")) }).containsExactly(first, first, second)
            assertThat(records.last().attributes.get(stringKey("session.previous_id"))).isEqualTo(first)
        }
    }
}
