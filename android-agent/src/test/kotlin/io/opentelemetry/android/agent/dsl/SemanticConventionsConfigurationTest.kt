/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.common.internal.SemconvCompat
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SemanticConventionsConfigurationTest {
    var compatState: Boolean = true

    @BeforeEach
    fun setup() {
        compatState = SemconvCompat.useLatestExperimental
    }

    @AfterEach
    fun restore() {
        SemconvCompat.useLatestExperimental = compatState
    }

    @Test
    fun `defaults to true`() {
        val rumConfig = mockk<OtelRumConfig>()
        every { rumConfig.setDiskBufferingConfig(any<DiskBufferingConfig>()) } returns rumConfig
        every { rumConfig.suppressInstrumentation(any<String>()) } returns rumConfig
        val otelConfig =
            OpenTelemetryConfiguration(
                rumConfig = rumConfig,
                instrumentationLoader = FakeInstrumentationLoader(),
                clock = FakeClock(),
            )
        assertThat(otelConfig.semanticConventions.useLatestExperimental).isTrue
        assertThat(SemconvCompat.useLatestExperimental).isTrue
    }

    @Test
    fun `can disable latest experimental semconv`() {
        val rumConfig = mockk<OtelRumConfig>()
        every { rumConfig.setDiskBufferingConfig(any<DiskBufferingConfig>()) } returns rumConfig
        every { rumConfig.suppressInstrumentation(any<String>()) } returns rumConfig
        val otelConfig =
            OpenTelemetryConfiguration(
                rumConfig = rumConfig,
                instrumentationLoader = FakeInstrumentationLoader(),
                clock = FakeClock(),
            )
        otelConfig.semanticConventions {
            useLatestExperimental = false
        }
        assertThat(SemconvCompat.useLatestExperimental).isFalse
    }
}
