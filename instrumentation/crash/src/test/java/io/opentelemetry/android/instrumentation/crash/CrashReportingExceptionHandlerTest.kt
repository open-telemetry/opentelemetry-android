/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CrashReportingExceptionHandlerTest {
    @Test
    fun shouldInvokeExistingHandlerWhenCrashProcessorThrows() {
        var existingHandlerCalled = false
        val existingHandler =
            Thread.UncaughtExceptionHandler { _, _ -> existingHandlerCalled = true }
        val handler =
            CrashReportingExceptionHandler(
                crashProcessor = { throw IllegalStateException("processor failed") },
                existingHandler = existingHandler,
            )

        runCatching { handler.uncaughtException(Thread.currentThread(), NullPointerException()) }

        assertThat(existingHandlerCalled).isTrue()
    }

    @Test
    fun shouldSkipCrashProcessorButInvokeExistingHandlerWhenDisabled() {
        var crashProcessorCalled = false
        var existingHandlerCalled = false
        val existingHandler =
            Thread.UncaughtExceptionHandler { _, _ -> existingHandlerCalled = true }
        val handler =
            CrashReportingExceptionHandler(
                crashProcessor = { crashProcessorCalled = true },
                existingHandler = existingHandler,
            )
        handler.enabled = false

        handler.uncaughtException(Thread.currentThread(), NullPointerException())

        assertThat(crashProcessorCalled).isFalse()
        assertThat(existingHandlerCalled).isTrue()
    }
}
