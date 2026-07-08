/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class OtelAndroidClockTest {
    private val gnssMillis = 1_000L
    private val networkMillis = 2_000L
    private val gnssClock = mockk<Clock> { every { millis() } returns gnssMillis }
    private val networkClock = mockk<Clock> { every { millis() } returns networkMillis }

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

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `uses GNSS time when available on API 29+`() {
        every { SystemClock.currentGnssTimeClock() } returns gnssClock

        val clock = OtelAndroidClock()

        assertThat(clock.now()).isEqualTo(gnssMillis * 1_000_000)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `prefers GNSS time over network time on API 33+`() {
        every { SystemClock.currentGnssTimeClock() } returns gnssClock
        every { SystemClock.currentNetworkTimeClock() } returns networkClock

        val clock = OtelAndroidClock()

        assertThat(clock.now()).isEqualTo(gnssMillis * 1_000_000)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `falls back to network time when GNSS clock throws on API 33+`() {
        every { SystemClock.currentGnssTimeClock() } throws RuntimeException("no gnss")
        every { SystemClock.currentNetworkTimeClock() } returns networkClock

        val clock = OtelAndroidClock()

        assertThat(clock.now()).isEqualTo(networkMillis * 1_000_000)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `falls back to system time when GNSS clock throws and network time is unavailable`() {
        every { SystemClock.currentGnssTimeClock() } throws RuntimeException("no gnss")

        val clock = OtelAndroidClock()

        assertThat(clock.now() / 1_000_000).isCloseTo(System.currentTimeMillis(), within(5000L))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `falls back to system time when both GNSS and network clocks throw on API 33+`() {
        every { SystemClock.currentGnssTimeClock() } throws RuntimeException("no gnss")
        every { SystemClock.currentNetworkTimeClock() } throws RuntimeException("no network")

        val clock = OtelAndroidClock()

        assertThat(clock.now() / 1_000_000).isCloseTo(System.currentTimeMillis(), within(5000L))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `uses system time directly below API 29`() {
        val clock = OtelAndroidClock()

        assertThat(clock.now() / 1_000_000).isCloseTo(System.currentTimeMillis(), within(5000L))
    }
}
