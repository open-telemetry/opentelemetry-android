/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class DiskBufferingConfigTest {
    private lateinit var otelConfig: OpenTelemetryConfiguration

    @BeforeEach
    fun setUp() {
        otelConfig =
            OpenTelemetryConfiguration(
                instrumentationLoader = FakeInstrumentationLoader(),
                clock = FakeClock(),
            )
    }

    @Test
    fun testDefaults() {
        assertThat(otelConfig.diskBufferingConfig.enabled).isTrue()
        assertThat(otelConfig.rumConfig.getDiskBufferingConfig().enabled).isTrue()
    }

    @Test
    @Suppress("DEPRECATION")
    fun `dsl defaults match the core defaults`() {
        assertThat(otelConfig.rumConfig.getDiskBufferingConfig())
            .isEqualTo(DiskBufferingConfig.create(enabled = true))
    }

    @Test
    fun `dsl exposes the core defaults`() {
        val spec = otelConfig.diskBufferingConfig
        assertThat(spec.maxCacheSize).isEqualTo(10 * 1024 * 1024)
        assertThat(spec.exportPeriod).isEqualTo(10.seconds)
        assertThat(spec.telemetryStorageDir).isNull()
    }

    @Test
    fun testOverride() {
        otelConfig.diskBuffering {
            enabled(false)
        }
        assertThat(otelConfig.diskBufferingConfig.enabled).isFalse()
        assertThat(otelConfig.rumConfig.getDiskBufferingConfig().enabled).isFalse()
    }

    @Test
    @Suppress("DEPRECATION")
    fun `applies every option to the rum config`() {
        val bufferDir = File("/tmp/otel-signals")
        otelConfig.diskBuffering {
            enabled(true)
            maxCacheSize = 5 * 1024 * 1024
            exportPeriod = 30.seconds
            telemetryStorageDir = bufferDir
            maxFileAgeForWrite = 40.seconds
            minFileAgeForRead = 50.seconds
            maxFileAgeForRead = 24.hours
            maxCacheFileSize = 2 * 1024
        }

        assertThat(otelConfig.rumConfig.getDiskBufferingConfig())
            .isEqualTo(
                DiskBufferingConfig.create(
                    enabled = true,
                    maxCacheSize = 5 * 1024 * 1024,
                    signalsBufferDir = bufferDir,
                    exportPeriodMillis = 30_000,
                    maxFileAgeForWriteMillis = 40_000,
                    minFileAgeForReadMillis = 50_000,
                    maxFileAgeForReadMillis = 24 * 60 * 60 * 1000,
                    maxCacheFileSize = 2 * 1024,
                ),
            )
    }

    @Test
    fun `re-entering the block keeps previously configured values`() {
        otelConfig.diskBuffering {
            maxCacheSize = 2048
        }
        otelConfig.diskBuffering {
            exportPeriod = 5.seconds
        }

        val config = otelConfig.rumConfig.getDiskBufferingConfig()
        assertThat(config.maxCacheSize).isEqualTo(2048)
        assertThat(config.exportPeriodMillis).isEqualTo(5_000)
    }

    @Test
    @Suppress("DEPRECATION")
    fun testDefaultFromRumConfig() {
        val diskBufferingConfig = DiskBufferingConfig.create(enabled = true)
        val rumConfig = OtelRumConfig()
        val otelConfig =
            OpenTelemetryConfiguration(
                rumConfig =
                    rumConfig.setDiskBufferingConfig(
                        diskBufferingConfig,
                    ),
                diskBufferingConfig = DiskBufferingConfigurationSpec(rumConfig),
                instrumentationLoader = FakeInstrumentationLoader(),
                clock = FakeClock(),
            )
        assertThat(otelConfig.diskBufferingConfig.enabled).isTrue()
        assertThat(otelConfig.rumConfig.getDiskBufferingConfig().enabled).isTrue()
    }
}
