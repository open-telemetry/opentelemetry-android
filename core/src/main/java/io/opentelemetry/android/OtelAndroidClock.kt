/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.opentelemetry.sdk.common.Clock

/**
 * An implementation of [Clock] that takes a baseline by subtracting
 * [SystemClock.elapsedRealtimeNanos] from the best available wall time and then
 * adds that baseline to [SystemClock.elapsedRealtimeNanos] to provide a monotonic wall-clock time.
 *
 * When selecting the anchor clock we try (in order):
 * - [SystemClock.currentGnssTimeClock]
 * - [SystemClock.currentNetworkTimeClock]
 * - [System.currentTimeMillis]
 *
 * This avoids the downside of [System.currentTimeMillis] not being monotonic on Android (i.e.
 * the clock doesn't consistently tick when the process is in the background or cached). It also
 * avoids the problem of [SystemClock.elapsedRealtimeNanos] not providing wall-clock time.
 */
class OtelAndroidClock : Clock {
    private val baselineNanos = currentTimeMillis() * 1_000_000 - nanoTime()

    override fun now(): Long = baselineNanos + nanoTime()

    override fun nanoTime(): Long = SystemClock.elapsedRealtimeNanos()

    private fun currentTimeMillis(): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return currentGnssTimeMillis()
            } catch (_: Exception) {
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                return currentNetworkTimeMillis()
            } catch (_: Exception) {
            }
        }
        
        return System.currentTimeMillis()
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun currentGnssTimeMillis(): Long = SystemClock.currentGnssTimeClock().millis()

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private fun currentNetworkTimeMillis(): Long = SystemClock.currentNetworkTimeClock().millis()
}
