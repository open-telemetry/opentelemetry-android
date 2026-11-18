/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.error

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
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `logs SdkBug as error with OTEL_RUM_LOG_TAG`() {
        val handler = DefaultSdkErrorHandler()
        val exception = RuntimeException("crash")

        handler.onError(SdkError.SdkBug(exception, "internal failure"))

        verify {
            Log.e(
                RumConstants.OTEL_RUM_LOG_TAG,
                "Internal SDK error: internal failure",
                exception,
            )
        }
    }

    @Test
    fun `logs UserCode as warning with OTEL_RUM_LOG_TAG`() {
        val handler = DefaultSdkErrorHandler()
        val exception = RuntimeException("user error")

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
        val exception = RuntimeException("custom")

        handler.onError(SdkError.SdkBug(exception, "test"))

        verify {
            Log.e(
                customTag,
                "Internal SDK error: test",
                exception,
            )
        }
    }

    @Test
    fun `INSTANCE singleton exists and uses default tag`() {
        assertThat(DefaultSdkErrorHandler.INSTANCE).isNotNull
    }
}
