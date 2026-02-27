/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.util.Log
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Installs a [Thread.UncaughtExceptionHandler] that force-flushes all signal
 * providers (traces, logs, metrics) before delegating to the previously set handler.
 *
 * This ensures that telemetry emitted during a crash (including the crash event itself)
 * is persisted before the process terminates, without requiring individual instrumentations
 * to access [OpenTelemetrySdk] directly.
 */
internal class CrashFlushHandler(
    private val sdk: OpenTelemetrySdk,
    private val flushTimeout: Duration = DEFAULT_FLUSH_TIMEOUT,
) {
    companion object {
        private val DEFAULT_FLUSH_TIMEOUT = 10.seconds
    }

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(
            FlushOnCrashExceptionHandler(
                sdk,
                Thread.getDefaultUncaughtExceptionHandler(),
                flushTimeout
            ),
        )
    }

    internal class FlushOnCrashExceptionHandler(
        private val sdk: OpenTelemetrySdk,
        private val previousHandler: Thread.UncaughtExceptionHandler?,
        private val flushTimeout: Duration,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(
            thread: Thread,
            throwable: Throwable,
        ) {
            // Let any previously installed handler run first (e.g. the crash
            // instrumentation emits the crash log record in its handler).
            previousHandler?.uncaughtException(thread, throwable)

            try {
                awaitCompletion(
                    flushTimeout,
                    sdk.sdkLoggerProvider.forceFlush(),
                    sdk.sdkTracerProvider.forceFlush(),
                    sdk.sdkMeterProvider.forceFlush()
                )
            } catch (e: Exception) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to flush telemetry on crash", e)
            }
        }

        private fun awaitCompletion(
            atMost: Duration,
            vararg completableItems: CompletableResultCode,
        ) {
            val latch = CountDownLatch(completableItems.size)
            for (completableResult in completableItems) {
                completableResult.whenComplete(latch::countDown)
            }
            try {
                latch.await(atMost.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
