/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiskBufferingConfigTest {
    @Test
    fun testDefaults() {
        val otelConfig = OpenTelemetryConfiguration(clock = FakeClock())
        assertTrue(otelConfig.diskBufferingConfig.enabled)
        assertTrue(otelConfig.rumConfig.getDiskBufferingConfig().enabled)
    }

    @Test
    fun testOverride() {
        val otelConfig = OpenTelemetryConfiguration(clock = FakeClock())
        otelConfig.diskBuffering {
            enabled(false)
        }
        assertFalse(otelConfig.diskBufferingConfig.enabled)
        assertFalse(otelConfig.rumConfig.getDiskBufferingConfig().enabled)
    }

    @Test
    fun testDefaultFromRumConfig() {
        val diskBufferingConfig = DiskBufferingConfig.create(enabled = true)
        val rumConfig = OtelRumConfig()
        val otelConfig =
            OpenTelemetryConfiguration(
                rumConfig.setDiskBufferingConfig(
                    diskBufferingConfig,
                ),
                DiskBufferingConfigurationSpec(rumConfig),
                FakeClock(),
            )
        assertTrue(otelConfig.diskBufferingConfig.enabled)
        assertTrue(otelConfig.rumConfig.getDiskBufferingConfig().enabled)
    }
}
