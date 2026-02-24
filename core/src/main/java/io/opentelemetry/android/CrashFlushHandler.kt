/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.util.Log
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.sdk.OpenTelemetrySdk
import java.util.concurrent.TimeUnit

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
    private val flushTimeoutMs: Long = DEFAULT_FLUSH_TIMEOUT_MS,
) {
    companion object {
        private const val DEFAULT_FLUSH_TIMEOUT_MS = 10_000L
    }

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(
            FlushOnCrashExceptionHandler(sdk, Thread.getDefaultUncaughtExceptionHandler(), flushTimeoutMs),
        )
    }

    internal class FlushOnCrashExceptionHandler(
        private val sdk: OpenTelemetrySdk,
        private val previousHandler: Thread.UncaughtExceptionHandler?,
        private val flushTimeoutMs: Long,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(
            thread: Thread,
            throwable: Throwable,
        ) {
            // Let any previously installed handler run first (e.g. the crash
            // instrumentation emits the crash log record in its handler).
            previousHandler?.uncaughtException(thread, throwable)

            try {
                sdk.sdkLoggerProvider.forceFlush().join(flushTimeoutMs, TimeUnit.MILLISECONDS)
                sdk.sdkTracerProvider.forceFlush().join(flushTimeoutMs, TimeUnit.MILLISECONDS)
                sdk.sdkMeterProvider.forceFlush().join(flushTimeoutMs, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to flush telemetry on crash", e)
            }
        }
    }
}
