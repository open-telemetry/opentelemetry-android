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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun `previous handler runs before flush`() {
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
            existingHandler.uncaughtException(thread, any())
            tracerProvider.forceFlush()
        }
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
