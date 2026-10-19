/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

internal class CrashReportingExceptionHandler(
    private val crashProcessor: (details: CrashDetails) -> Unit,
    internal val existingHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler(),
) : Thread.UncaughtExceptionHandler {
    @Volatile
    var enabled: Boolean = true

    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        if (enabled) {
            crashProcessor(CrashDetails(thread, throwable))
        }

        // preserve any existing behavior
        existingHandler?.uncaughtException(thread, throwable)
    }
}
