package io.opentelemetry.android.instrumentation.anr

import android.os.Handler
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AnrWatcherTest {
    @JvmField
    @RegisterExtension
    val testing = OpenTelemetryExtension.create()

    private lateinit var handler: Handler
    private lateinit var mainThread: Thread
    private lateinit var instrumenter: Instrumenter<Array<StackTraceElement>, Void>

    @Before
    fun setup() {
        handler = mockk()
        mainThread = mockk()
        instrumenter = mockk()
    }

    @Test
    fun mainThreadDisappearing() {
        val anrWatcher = AnrWatcher(handler, mainThread, instrumenter)
        for (i in 0..4) {
            every { handler.post(any()) } returns false
            anrWatcher.run()
        }
        verify { instrumenter wasNot Called }
    }

    @Test
    fun noAnr() {
        val anrWatcher = AnrWatcher(handler, mainThread, instrumenter)
        for (i in 0..4) {
            every { handler.post(any()) } answers {
                val callback = it.invocation.args[0] as Runnable
                callback.run()
                true
            }

            anrWatcher.run()
        }
        verify { instrumenter wasNot Called }
    }

    @Test
    fun noAnr_temporaryPause() {
        val anrWatcher = AnrWatcher(handler, mainThread, instrumenter)
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
        verify { instrumenter wasNot Called }
    }

    @Test
    fun anr_detected() {
        val stackTrace: Array<StackTraceElement> = arrayOf()
        every { instrumenter.start(any(), any()) } returns mockk()
        every { instrumenter.end(any(), any(), any(), any()) } returns mockk()
        every { mainThread.stackTrace } returns stackTrace

        val anrWatcher = AnrWatcher(handler, mainThread, instrumenter)
        every { handler.post(any()) } returns true
        for (i in 0..4) {
            anrWatcher.run()
        }
        verify(exactly = 1) { instrumenter.start(any(), refEq(stackTrace)) }
        verify(exactly = 1) { instrumenter.end(any(), refEq(stackTrace), isNull(), isNull()) }
        for (i in 0..3) {
            anrWatcher.run()
        }
        // Still just the 1 time
        verify(exactly = 1) { instrumenter.start(any(), refEq(stackTrace)) }
        verify(exactly = 1) { instrumenter.end(any(), refEq(stackTrace), isNull(), isNull()) }

        anrWatcher.run()

        verify(exactly = 2) { instrumenter.start(any(), refEq(stackTrace)) }
        verify(exactly = 2) { instrumenter.end(any(), refEq(stackTrace), isNull(), isNull()) }
    }
}