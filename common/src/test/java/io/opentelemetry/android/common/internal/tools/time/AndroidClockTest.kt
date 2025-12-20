/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.tools.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndroidClockTest {
    @Test
    fun `nanoTime returns baseline plus elapsed realtime`() {
        val wallClock = 1000L
        val elapsedRealtime = 500L

        val clock =
            AndroidClock(
                timeSinceEpochMillisProvider = { wallClock },
                timeHighPrecisionMillisProvider = { elapsedRealtime },
            )

        val expectedBaseline = wallClock - elapsedRealtime
        val expectedNanoTime = expectedBaseline + elapsedRealtime

        assertThat(clock.nanoTime()).isEqualTo(expectedNanoTime)
    }

    @Test
    fun `nanoTime increases as elapsed realtime increases`() {
        val wallClock = 1000L
        var elapsedRealtime = 500L

        val clock =
            AndroidClock(
                timeSinceEpochMillisProvider = { wallClock },
                timeHighPrecisionMillisProvider = { elapsedRealtime },
            )

        val firstNanoTime = clock.nanoTime()
        elapsedRealtime = 600L
        val expectedBaseLine = wallClock - elapsedRealtime
        val secondNanoTime = clock.nanoTime()

        assertThat(secondNanoTime).isGreaterThan(elapsedRealtime + expectedBaseLine)
        assertThat(secondNanoTime - firstNanoTime).isEqualTo(100L)
    }
}
