/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

class CrashFlushHandlerTest {
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    @BeforeEach
    fun setUp() {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    }

    @AfterEach
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }

    @Test
    fun `installs as default uncaught exception handler`() {
        val sdk = mockSdk()
        CrashFlushHandler(sdk).install()

        val handler = Thread.getDefaultUncaughtExceptionHandler()
        assertThat(handler).isInstanceOf(CrashFlushHandler.FlushOnCrashExceptionHandler::class.java)
    }

    @Test
    fun `delegates to previous handler`() {
        val existingHandler = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
        Thread.setDefaultUncaughtExceptionHandler(existingHandler)

        val sdk = mockSdk()
        CrashFlushHandler(sdk).install()

        val handler = Thread.getDefaultUncaughtExceptionHandler()!!
        val exception = RuntimeException("test")
        val thread = Thread.currentThread()
        handler.uncaughtException(thread, exception)

        verify { existingHandler.uncaughtException(thread, exception) }
    }

    @Test
    fun `flushes all signal providers on crash`() {
        val tracerProvider = mockk<SdkTracerProvider>()
        val loggerProvider = mockk<SdkLoggerProvider>()
        val meterProvider = mockk<SdkMeterProvider>()
        val sdk = mockSdk(tracerProvider, loggerProvider, meterProvider)

        every { tracerProvider.forceFlush() } returns CompletableResultCode.ofSuccess()
        every { loggerProvider.forceFlush() } returns CompletableResultCode.ofSuccess()
        every { meterProvider.forceFlush() } returns CompletableResultCode.ofSuccess()

        CrashFlushHandler(sdk).install()

        val handler = Thread.getDefaultUncaughtExceptionHandler()!!
        handler.uncaughtException(Thread.currentThread(), RuntimeException("test"))

        verify { tracerProvider.forceFlush() }
        verify { loggerProvider.forceFlush() }
        verify { meterProvider.forceFlush() }
    }

    @Test
    fun `does not throw when flush fails`() {
        val tracerProvider = mockk<SdkTracerProvider>()
        val loggerProvider = mockk<SdkLoggerProvider>()
        val meterProvider = mockk<SdkMeterProvider>()
        val sdk = mockSdk(tracerProvider, loggerProvider, meterProvider)

        every { loggerProvider.forceFlush() } throws RuntimeException("flush failed")

        CrashFlushHandler(sdk).install()

        val handler = Thread.getDefaultUncaughtExceptionHandler()!!
        handler.uncaughtException(Thread.currentThread(), RuntimeException("test"))

        verify { loggerProvider.forceFlush() }
        verify { tracerProvider wasNot Called }
        verify { meterProvider wasNot Called }
    }

    @Test
    fun `still delegates to previous handler when flush fails`() {
        val existingHandler = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
        Thread.setDefaultUncaughtExceptionHandler(existingHandler)

        val loggerProvider = mockk<SdkLoggerProvider>()
        val sdk = mockSdk(loggerProvider = loggerProvider)
        every { loggerProvider.forceFlush() } throws RuntimeException("flush failed")

        CrashFlushHandler(sdk).install()

        val handler = Thread.getDefaultUncaughtExceptionHandler()!!
        val thread = Thread.currentThread()
        val exception = RuntimeException("test")
        handler.uncaughtException(thread, exception)

        verify { existingHandler.uncaughtException(thread, exception) }
    }

    @Test
    fun `still delegates to previous handler when flush throws an error`() {
        val existingHandler = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
        Thread.setDefaultUncaughtExceptionHandler(existingHandler)

        val loggerProvider = mockk<SdkLoggerProvider>()
        val sdk = mockSdk(loggerProvider = loggerProvider)
        // Errors are not caught, so the handler exits by propagating; the
        // finally block must still delegate on the way out.
        every { loggerProvider.forceFlush() } throws StackOverflowError("flush failed")

        CrashFlushHandler(sdk).install()

        val handler = Thread.getDefaultUncaughtExceptionHandler()!!
        val thread = Thread.currentThread()
        val exception = RuntimeException("test")

        assertThatThrownBy { handler.uncaughtException(thread, exception) }
            .isInstanceOf(StackOverflowError::class.java)

        verify { existingHandler.uncaughtException(thread, exception) }
    }

    @Test
    fun `propagates flush error when there is no previous handler`() {
        Thread.setDefaultUncaughtExceptionHandler(null)

        val loggerProvider = mockk<SdkLoggerProvider>()
        val sdk = mockSdk(loggerProvider = loggerProvider)
        every { loggerProvider.forceFlush() } throws StackOverflowError("flush failed")

        CrashFlushHandler(sdk).install()

        val handler = Thread.getDefaultUncaughtExceptionHandler()!!

        assertThatThrownBy { handler.uncaughtException(Thread.currentThread(), RuntimeException("test")) }
            .isInstanceOf(StackOverflowError::class.java)
    }

    @Test
    fun `flush completes before previous handler runs`() {
        val existingHandler = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
        Thread.setDefaultUncaughtExceptionHandler(existingHandler)

        val tracerProvider = mockk<SdkTracerProvider>()
        val loggerProvider = mockk<SdkLoggerProvider>()
        val meterProvider = mockk<SdkMeterProvider>()
        val sdk = mockSdk(tracerProvider, loggerProvider, meterProvider)

        every { tracerProvider.forceFlush() } returns CompletableResultCode.ofSuccess()
        every { loggerProvider.forceFlush() } returns CompletableResultCode.ofSuccess()
        every { meterProvider.forceFlush() } returns CompletableResultCode.ofSuccess()

        CrashFlushHandler(sdk).install()

        val handler = Thread.getDefaultUncaughtExceptionHandler()!!
        val thread = Thread.currentThread()
        handler.uncaughtException(thread, RuntimeException("test"))

        verifyOrder {
            loggerProvider.forceFlush()
            tracerProvider.forceFlush()
            meterProvider.forceFlush()
            existingHandler.uncaughtException(thread, any())
        }
    }

    @Test
    fun `flush is awaited before previous handler runs`() {
        val flushResult = CompletableResultCode()
        val flushObservedIncomplete = AtomicBoolean(false)
        val existingHandler =
            Thread.UncaughtExceptionHandler { _, _ ->
                flushObservedIncomplete.set(!flushResult.isDone)
            }
        Thread.setDefaultUncaughtExceptionHandler(existingHandler)

        val tracerProvider = mockk<SdkTracerProvider>()
        val loggerProvider = mockk<SdkLoggerProvider>()
        val meterProvider = mockk<SdkMeterProvider>()
        val sdk = mockSdk(tracerProvider, loggerProvider, meterProvider)

        every { loggerProvider.forceFlush() } returns flushResult
        every { tracerProvider.forceFlush() } returns CompletableResultCode.ofSuccess()
        every { meterProvider.forceFlush() } returns CompletableResultCode.ofSuccess()

        CrashFlushHandler(sdk, flushTimeout = 10.seconds).install()

        val handler = Thread.getDefaultUncaughtExceptionHandler()!!
        val crashThread =
            Thread {
                handler.uncaughtException(Thread.currentThread(), RuntimeException("test"))
            }
        crashThread.start()

        // The handler must still be blocked on the incomplete flush.
        crashThread.join(500)
        assertThat(crashThread.isAlive).isTrue()

        flushResult.succeed()
        crashThread.join(5_000)
        assertThat(crashThread.isAlive).isFalse()
        assertThat(flushObservedIncomplete).isFalse()
    }

    private fun mockSdk(
        tracerProvider: SdkTracerProvider = mockk(relaxed = true),
        loggerProvider: SdkLoggerProvider = mockk(relaxed = true),
        meterProvider: SdkMeterProvider = mockk(relaxed = true),
    ): OpenTelemetrySdk {
        val sdk = mockk<OpenTelemetrySdk>()
        every { sdk.sdkTracerProvider } returns tracerProvider
        every { sdk.sdkLoggerProvider } returns loggerProvider
        every { sdk.sdkMeterProvider } returns meterProvider
        return sdk
    }
}
