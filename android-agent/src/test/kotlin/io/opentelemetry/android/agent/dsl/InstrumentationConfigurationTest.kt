package io.opentelemetry.android.agent.dsl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
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
        otelConfig = OpenTelemetryConfiguration(
            rumConfig = rumConfig,
            instrumentationLoader = FakeInstrumentationLoader(),
            clock = FakeClock()
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
}
