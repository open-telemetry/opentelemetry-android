/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.crash.CrashReporterInstrumentation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class InstrumentationConfigurationTest {
    private lateinit var otelConfig: OpenTelemetryConfiguration
    private lateinit var rumConfig: OtelRumConfig

    @Before
    fun setUp() {
        rumConfig = mockk()
        every { rumConfig.setDiskBufferingConfig(any<DiskBufferingConfig>()) } returns rumConfig
        every { rumConfig.suppressInstrumentation(any<String>()) } returns rumConfig
        every { rumConfig.disableInstrumentationDiscovery() } returns rumConfig
        otelConfig =
            OpenTelemetryConfiguration(
                rumConfig = rumConfig,
                instrumentationLoader = FakeInstrumentationLoader(),
                clock = FakeClock(),
            )
    }

    @Test
    fun canSuppressInstrumentation() {
        otelConfig.instrumentations {
            suppressing("one", "two", "three")
        }
        verify {
            rumConfig.suppressInstrumentation("one")
            rumConfig.suppressInstrumentation("two")
            rumConfig.suppressInstrumentation("three")
        }
    }

    @Test
    fun canSuppressAndAllowCrashReporterByItsName() {
        val config = OtelRumConfig()
        val crashReporter = CrashReporterInstrumentation()
        val loader = mockk<AndroidInstrumentationLoader>()
        every { loader.getByType(CrashReporterInstrumentation::class.java) } returns crashReporter
        val configuration =
            OpenTelemetryConfiguration(
                rumConfig = config,
                instrumentationLoader = loader,
                clock = FakeClock(),
            )

        configuration.instrumentations {
            suppressing("java-crash")
        }
        assertThat(config.isSuppressed("java-crash")).isTrue()

        configuration.instrumentations {
            crashReporter { enabled(true) }
        }
        assertThat(config.isSuppressed("java-crash")).isFalse()
    }

    @Test
    fun canDisableInstrumentationAutoDiscovery() {
        otelConfig.instrumentations {
            disableInstrumentationAutoDiscovery()
        }

        verify { rumConfig.disableInstrumentationDiscovery() }
    }
}
