/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import android.os.Handler
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.semconv.events.DeviceAnrEvent.Companion.DEVICE_ANR_EVENT_NAME
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AnrWatcherTest {
    companion object {
        @JvmField
        @RegisterExtension
        val testing: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private lateinit var handler: Handler
    private lateinit var mainThread: Thread
    private lateinit var logger: Logger
    private lateinit var logRecordBuilder: LogRecordBuilder

    @Before
    fun setup() {
        handler = mockk()
        mainThread = Thread.currentThread()
        logger = mockk()
        logRecordBuilder = mockk(relaxed = true)

        every { logger.logRecordBuilder() } returns logRecordBuilder
        every { logRecordBuilder.setEventName(any()) } returns logRecordBuilder
        every { logRecordBuilder.setAllAttributes(any<Attributes>()) } returns logRecordBuilder
        every { logRecordBuilder.emit() } returns Unit
    }

    @Test
    fun mainThreadDisappearing() {
        val anrWatcher = AnrWatcher(handler, mainThread, logger)
        for (i in 0..4) {
            every { handler.post(any()) } returns false
            anrWatcher.run()
        }
        verify { logger wasNot Called }
    }

    @Test
    fun noAnr() {
        val anrWatcher = AnrWatcher(handler, mainThread, logger)
        for (i in 0..4) {
            every { handler.post(any()) } answers {
                val callback = it.invocation.args[0] as Runnable
                callback.run()
                true
            }

            anrWatcher.run()
        }
        verify { logger wasNot Called }
    }

    @Test
    fun noAnr_temporaryPause() {
        val anrWatcher = AnrWatcher(handler, mainThread, logger)
        for (i in 0..4) {
            val index = i
            every { handler.post(any()) } answers {
                val callback = it.invocation.args[0] as Runnable
                // have it fail once
                if (index != 3) {
                    callback.run()
                }
                true
            }
            anrWatcher.run()
        }
        verify { logger wasNot Called }
    }

    @Test
    fun anr_detected() {
        val anrWatcher = AnrWatcher(handler, mainThread, logger, emptyList(), 1)
        every { handler.post(any()) } returns true

        for (i in 0..4) {
            anrWatcher.run()
        }
        verify(exactly = 1) { logger.logRecordBuilder() }
        verify(exactly = 1) { logRecordBuilder.setEventName(DEVICE_ANR_EVENT_NAME) }
        verify(exactly = 1) { logRecordBuilder.setAllAttributes(any<Attributes>()) }
        verify(exactly = 1) { logRecordBuilder.emit() }

        for (i in 0..3) {
            anrWatcher.run()
        }
        // Still just the 1 time
        verify(exactly = 1) { logger.logRecordBuilder() }
        verify(exactly = 1) { logRecordBuilder.emit() }

        anrWatcher.run()

        verify(exactly = 2) { logger.logRecordBuilder() }
        verify(exactly = 2) { logRecordBuilder.setEventName(DEVICE_ANR_EVENT_NAME) }
        verify(exactly = 2) { logRecordBuilder.setAllAttributes(any<Attributes>()) }
        verify(exactly = 2) { logRecordBuilder.emit() }
    }

    @Test
    fun anr_extractorThrows_doesNotPropagateAndKeepsWatching() {
        var calls = 0
        val extractor =
            EventAttributesExtractor<Array<StackTraceElement>> { _, _ ->
                calls++
                check(calls > 1) { "boom" }
                Attributes.empty()
            }
        val anrWatcher = AnrWatcher(handler, mainThread, logger, listOf(extractor), 1)
        every { handler.post(any()) } returns true

        // first detection cycle: the extractor throws, nothing is emitted and run() does not throw
        for (i in 0..4) {
            anrWatcher.run()
        }
        assertThat(calls).isEqualTo(1)
        verify(exactly = 0) { logRecordBuilder.emit() }

        // the counter was reset, so four more polls are not enough for another detection
        for (i in 0..3) {
            anrWatcher.run()
        }
        assertThat(calls).isEqualTo(1)
        verify(exactly = 0) { logRecordBuilder.emit() }

        // the fifth poll completes the next cycle and the event is emitted
        anrWatcher.run()
        assertThat(calls).isEqualTo(2)
        verify(exactly = 1) { logRecordBuilder.emit() }
    }
}
