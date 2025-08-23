/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

internal class CrashReportingExceptionHandler(
    private val crashProcessor: (details: CrashDetails) -> Unit,
    private val postCrashAction: () -> Unit,
    private val existingHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler(),
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        crashProcessor(CrashDetails(thread, throwable))

        // do our best to make sure the crash makes it out of the VM
        postCrashAction()

        // preserve any existing behavior
        existingHandler?.uncaughtException(thread, throwable)
    }
}
