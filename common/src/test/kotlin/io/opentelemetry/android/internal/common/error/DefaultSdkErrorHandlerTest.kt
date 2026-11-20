/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.common.error

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.opentelemetry.android.common.RumConstants
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultSdkErrorHandlerTest {
    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `logs SdkOperation as warning with OTEL_RUM_LOG_TAG`() {
        val handler = DefaultSdkErrorHandler()
        val exception = IllegalStateException("crash")

        handler.onError(SdkError.SdkOperation(exception, "internal failure"))

        verify {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "Internal SDK error: internal failure",
                exception,
            )
        }
    }

    @Test
    fun `logs UserCode as warning with OTEL_RUM_LOG_TAG`() {
        val handler = DefaultSdkErrorHandler()
        val exception = IllegalStateException("user error")

        handler.onError(SdkError.UserCode(exception, "callback failed"))

        verify {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "Error in user-provided code: callback failed",
                exception,
            )
        }
    }

    @Test
    fun `logs ApiMisuse as warning with OTEL_RUM_LOG_TAG`() {
        val handler = DefaultSdkErrorHandler()
        val exception = IllegalArgumentException("bad argument")

        handler.onError(SdkError.ApiMisuse(exception, "misconfigured builder"))

        verify {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "API misuse error: misconfigured builder",
                exception,
            )
        }
    }

    @Test
    fun `custom logTag is respected`() {
        val customTag = "CustomTag"
        val handler = DefaultSdkErrorHandler(logTag = customTag)
        val exception = IllegalStateException("custom")

        handler.onError(SdkError.SdkOperation(exception, "test"))

        verify {
            Log.w(
                customTag,
                "Internal SDK error: test",
                exception,
            )
        }
    }

    @Test
    fun `rate limits repetitive error logs within period`() {
        var currentTime = 1000L
        val handler =
            DefaultSdkErrorHandler(
                maxEventsPerPeriod = 2,
                periodMs = 1000L,
                clock = { currentTime },
            )
        val exception = IllegalStateException("boom")

        handler.onError(SdkError.SdkOperation(exception, "err1"))
        handler.onError(SdkError.SdkOperation(exception, "err2"))
        handler.onError(SdkError.SdkOperation(exception, "err3"))
        handler.onError(SdkError.SdkOperation(exception, "err4"))

        verify(exactly = 1) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Internal SDK error: err1", exception)
        }
        verify(exactly = 1) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Internal SDK error: err2", exception)
        }
        verify(exactly = 0) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Internal SDK error: err3", exception)
        }
        verify(exactly = 0) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Internal SDK error: err4", exception)
        }

        currentTime = 2500L
        handler.onError(SdkError.SdkOperation(exception, "err5"))

        verify(exactly = 1) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Suppressed 2 repetitive SDK error logs")
        }
        verify(exactly = 1) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Internal SDK error: err5", exception)
        }
    }

    @Test
    fun `INSTANCE singleton exists and uses default tag`() {
        assertThat(DefaultSdkErrorHandler.INSTANCE).isNotNull
    }
}
