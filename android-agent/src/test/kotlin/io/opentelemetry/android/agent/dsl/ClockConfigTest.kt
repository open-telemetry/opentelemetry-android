/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.sdk.common.Clock
import org.junit.Assert.assertSame
import org.junit.Test

class ClockConfigTest {
    @Test
    fun testDefaults() {
        val otelConfig = OpenTelemetryConfiguration()
        assertSame(Clock.getDefault(), otelConfig.clock)
    }

    @Test
    fun testOverride() {
        val fakeClock =
            object : Clock {
                override fun now(): Long = 0

                override fun nanoTime(): Long = 0
            }
        val otelConfig =
            OpenTelemetryConfiguration().apply {
                clock = fakeClock
            }
        assertSame(fakeClock, otelConfig.clock)
    }
}
