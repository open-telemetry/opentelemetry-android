/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.OtelAndroidClock
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockConfigTest {
    @Test
    fun testDefaults() {
        val otelConfig = OpenTelemetryConfiguration()
        assertThat(otelConfig.clock).isInstanceOf(OtelAndroidClock::class.java)
    }

    @Test
    fun testOverride() {
        val fakeClock = FakeClock()
        val otelConfig =
            OpenTelemetryConfiguration().apply {
                clock = fakeClock
            }
        assertThat(otelConfig.clock).isSameAs(fakeClock)
    }
}
