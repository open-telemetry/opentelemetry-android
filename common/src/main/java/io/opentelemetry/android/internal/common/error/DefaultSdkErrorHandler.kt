/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.common.error

import android.os.SystemClock
import android.util.Log
import io.opentelemetry.android.common.RumConstants

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 *
 * Default implementation of [SdkErrorHandler] that logs errors via Android [Log] under [logTag],
 * with rate limiting to prevent Logcat flooding from repetitive errors.
 */
class DefaultSdkErrorHandler(
    private val logTag: String = RumConstants.OTEL_RUM_LOG_TAG,
    private val maxEventsPerPeriod: Int = DEFAULT_MAX_LOGS_PER_PERIOD,
    private val periodMs: Long = DEFAULT_PERIOD_MS,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : SdkErrorHandler {
    private val lock = Any()
    private var windowStartMs: Long = clock()
    private var loggedCountInWindow: Int = 0
    private var suppressedCount: Int = 0

    override fun onError(error: SdkError) {
        val (shouldLog, currentSuppressed) =
            synchronized(lock) {
                val now = clock()
                if (now - windowStartMs >= periodMs) {
                    windowStartMs = now
                    val suppressed = suppressedCount
                    suppressedCount = 0
                    loggedCountInWindow = 1
                    Pair(true, suppressed)
                } else if (loggedCountInWindow < maxEventsPerPeriod) {
                    loggedCountInWindow++
                    Pair(true, 0)
                } else {
                    suppressedCount++
                    Pair(false, 0)
                }
            }

        if (!shouldLog) {
            return
        }

        if (currentSuppressed > 0) {
            Log.w(logTag, "Suppressed $currentSuppressed repetitive SDK error logs")
        }

        when (error) {
            is SdkError.SdkOperation -> {
                Log.w(logTag, "Internal SDK error: ${error.description}", error.throwable)
            }

            is SdkError.UserCode -> {
                Log.w(logTag, "Error in user-provided code: ${error.description}", error.throwable)
            }

            is SdkError.ApiMisuse -> {
                Log.w(logTag, "API misuse error: ${error.description}", error.throwable)
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_LOGS_PER_PERIOD: Int = 10
        const val DEFAULT_PERIOD_MS: Long = 1000L

        val INSTANCE: DefaultSdkErrorHandler = DefaultSdkErrorHandler()
    }
}
