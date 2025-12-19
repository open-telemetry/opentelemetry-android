/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.OtelAndroidClock
import io.opentelemetry.android.agent.FakeClock
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockConfigTest {
    @Test
    fun testDefaults() {
        val otelConfig = OpenTelemetryConfiguration()
        assertTrue(otelConfig.clock is OtelAndroidClock)
    }

    @Test
    fun testOverride() {
        val fakeClock = FakeClock()
        val otelConfig =
            OpenTelemetryConfiguration().apply {
                clock = fakeClock
            }
        assertSame(fakeClock, otelConfig.clock)
    }
}
