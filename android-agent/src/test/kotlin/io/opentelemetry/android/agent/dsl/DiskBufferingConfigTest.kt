/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiskBufferingConfigTest {
    @Test
    fun testDefaults() {
        val otelConfig = OpenTelemetryConfiguration()
        assertTrue(otelConfig.diskBufferingConfig.enabled)
        assertTrue(otelConfig.rumConfig.getDiskBufferingConfig().enabled)
    }

    @Test
    fun testOverride() {
        val otelConfig = OpenTelemetryConfiguration()
        otelConfig.diskBuffering {
            enabled(false)
        }
        assertFalse(otelConfig.diskBufferingConfig.enabled)
        assertFalse(otelConfig.rumConfig.getDiskBufferingConfig().enabled)
    }

    @Test
    fun testDefaultFromRumConfig() {
        val otelConfig =
            OpenTelemetryConfiguration(
                OtelRumConfig().setDiskBufferingConfig(
                    DiskBufferingConfig.create(enabled = true),
                ),
            )
        assertTrue(otelConfig.diskBufferingConfig.enabled)
        assertTrue(otelConfig.rumConfig.getDiskBufferingConfig().enabled)
    }

    @Test
    fun testOverrideExportDelayAndAutoDetect() {
        val otelConfig = OpenTelemetryConfiguration()
        otelConfig.diskBuffering {
            enabled(true)
            exportScheduleDelay(100)
            autoDetectExportSchedule(true)
        }
        assertTrue(otelConfig.diskBufferingConfig.enabled)
        assertTrue(otelConfig.rumConfig.getDiskBufferingConfig().enabled)
        assertTrue(otelConfig.diskBufferingConfig.autoDetectExportSchedule)
        assertTrue(otelConfig.rumConfig.getDiskBufferingConfig().autoDetectExportSchedule)

        // DSL helper stores the raw value
        assert(otelConfig.diskBufferingConfig.exportScheduleDelay == 100L)
        // Config object enforces minimum 1000ms
        assert(otelConfig.rumConfig.getDiskBufferingConfig().exportScheduleDelayMillis == 1000L)
    }

    @Test
    fun testOverrideExportDelayAndAutoDetectValid() {
        val otelConfig = OpenTelemetryConfiguration()
        otelConfig.diskBuffering {
            enabled(true)
            exportScheduleDelay(5000)
            autoDetectExportSchedule(true)
        }

        assertTrue(otelConfig.diskBufferingConfig.autoDetectExportSchedule)
        assertTrue(otelConfig.rumConfig.getDiskBufferingConfig().autoDetectExportSchedule)

        // Check delay matches what was set since it's > 1000ms
        val storedConfig = otelConfig.rumConfig.getDiskBufferingConfig()
        assert(storedConfig.exportScheduleDelayMillis == 5000L)
    }
}
