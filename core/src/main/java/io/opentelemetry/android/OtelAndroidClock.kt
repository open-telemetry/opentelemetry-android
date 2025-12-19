/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.os.SystemClock
import io.opentelemetry.sdk.common.Clock

/**
 * An implementation of [Clock] that takes a baseline by subtracting
 * [SystemClock.elapsedRealtimeNanos] from [System.currentTimeMillis] and then
 * adds that baseline to [SystemClock.elapsedRealtimeNanos] to provide a monotonic wall-clock time.
 *
 * This avoids the downside of [System.currentTimeMillis] not being monotonic on Android (i.e.
 * the clock doesn't consistently tick when the process is in the backround or cached). It also
 * avoids the problem of [SystemClock.elapsedRealtimeNanos] not providing wall-clock time.
 */
class OtelAndroidClock : Clock {
    private val baseline = System.currentTimeMillis() - nanoTime()

    override fun now(): Long = baseline + nanoTime()

    override fun nanoTime(): Long = SystemClock.elapsedRealtimeNanos()
}
