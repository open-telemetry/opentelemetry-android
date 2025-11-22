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
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AnrWatcherTest {
    companion object {
        @JvmField
        @RegisterExtension
        val testing = OpenTelemetryExtension.create()
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
        val anrWatcher = AnrWatcher(handler, mainThread, logger, SessionProvider.getNoop())
        for (i in 0..4) {
            every { handler.post(any()) } returns false
            anrWatcher.run()
        }
        verify { logger wasNot Called }
    }

    @Test
    fun noAnr() {
        val anrWatcher = AnrWatcher(handler, mainThread, logger, SessionProvider.getNoop())
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
        val anrWatcher = AnrWatcher(handler, mainThread, logger, SessionProvider.getNoop())
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
        val anrWatcher = AnrWatcher(handler, mainThread, logger, SessionProvider.getNoop(), emptyList(), 1)
        every { handler.post(any()) } returns true

        for (i in 0..4) {
            anrWatcher.run()
        }
        verify(exactly = 1) { logger.logRecordBuilder() }
        verify(exactly = 1) { logRecordBuilder.setEventName("device.anr") }
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
        verify(exactly = 2) { logRecordBuilder.setEventName("device.anr") }
        verify(exactly = 2) { logRecordBuilder.setAllAttributes(any<Attributes>()) }
        verify(exactly = 2) { logRecordBuilder.emit() }
    }
}
