/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * A [PermissionGrantCheckTrigger] that checks the permission when asked. Callers ask on every span
 * and log, and before API 30 each check is a binder call to the system, so checks are throttled.
 * Apps usually ask for permissions right after launch, so during [STARTUP_WINDOW_MS] calls check
 * at most once per [STARTUP_CHECK_INTERVAL_MS], which caps that window at 10 checks while still
 * seeing a grant within a few seconds. After that, calls check at most once per
 * [CHECK_INTERVAL_MS].
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
internal class PollingPermissionGrantCheckTrigger(
    private val isGranted: () -> Boolean,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : PermissionGrantCheckTrigger {
    private val startedAt = clock()

    // Revoking a runtime permission kills the process, so this only goes from true to false.
    private val awaitingGrant = AtomicBoolean(!isGranted())
    private val nextCheckAt = AtomicLong(0)

    override fun consumeTrigger(): Boolean =
        awaitingGrant.get() &&
            isCheckDue() &&
            isGranted() &&
            awaitingGrant.compareAndSet(true, false)

    private fun isCheckDue(): Boolean {
        val now = clock()
        val next = nextCheckAt.get()
        val interval =
            if (now - startedAt < STARTUP_WINDOW_MS) STARTUP_CHECK_INTERVAL_MS else CHECK_INTERVAL_MS
        return now >= next && nextCheckAt.compareAndSet(next, now + interval)
    }

    companion object {
        const val STARTUP_WINDOW_MS = 30_000L
        const val STARTUP_CHECK_INTERVAL_MS = 3_000L
        const val CHECK_INTERVAL_MS = 10_000L
    }
}
