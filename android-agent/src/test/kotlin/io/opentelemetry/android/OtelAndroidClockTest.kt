/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.os.SystemClock
import io.mockk.every
import io.mockk.mockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class OtelAndroidClockTest {
    @Before
    fun setup() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtimeNanos() } returns 1_000_000
    }

    @Test
    fun `now() provides nanos`() {
        val clock = OtelAndroidClock()
        val clockNowNanos = clock.now() // returns nanos
        val clockNowMillis = clockNowNanos / 1_000_000 // convert nanos to ms
        val currentTimeMillis = System.currentTimeMillis() // returns millis
        assertThat(currentTimeMillis - clockNowMillis).isLessThan(5000)
    }
}
