/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.OtelAndroidClock
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockConfigTest {

    private lateinit var otelConfig: OpenTelemetryConfiguration

    @Before
    fun setUp() {
        otelConfig = OpenTelemetryConfiguration(instrumentationLoader = FakeInstrumentationLoader())
    }

    @Test
    fun testDefaults() {
        assertThat(otelConfig.clock).isInstanceOf(OtelAndroidClock::class.java)
    }

    @Test
    fun testOverride() {
        val fakeClock = FakeClock()
        val otelConfig = otelConfig.apply {
            clock = fakeClock
        }
        assertThat(otelConfig.clock).isSameAs(fakeClock)
    }
}
