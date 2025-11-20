/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenTelemetryConfigurationTest {
    @Test
    fun `Check diskBuffering default configuration is applied`() {
        val rumConfig = mockk<OtelRumConfig>(relaxed = true)
        OpenTelemetryConfiguration(rumConfig)

        val slot = slot<DiskBufferingConfig>()
        verify { rumConfig.setDiskBufferingConfig(capture(slot)) }

        assertTrue(slot.captured.enabled)
    }
}
