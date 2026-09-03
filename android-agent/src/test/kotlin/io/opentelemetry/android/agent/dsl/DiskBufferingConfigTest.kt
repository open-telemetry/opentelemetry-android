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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
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
    fun `dsl defaults match the core defaults`() {
        assertThat(otelConfig.rumConfig.getDiskBufferingConfig())
            .isEqualTo(DiskBufferingConfig.create(enabled = true))
    }

    @Test
    fun `dsl exposes the core defaults`() {
        val spec = otelConfig.diskBufferingConfig
        assertThat(spec.maxCacheSize).isEqualTo(10 * 1024 * 1024)
        assertThat(spec.maxCacheFileSize).isEqualTo(1024 * 1024)
        assertThat(spec.maxFileAgeForWrite).isEqualTo(30.seconds)
        assertThat(spec.minFileAgeForRead).isEqualTo(33.seconds)
        assertThat(spec.maxFileAgeForRead).isEqualTo(18.hours)
        assertThat(spec.exportPeriod).isEqualTo(10.seconds)
        assertThat(spec.signalsBufferDir).isNull()
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
    fun `applies every option to the rum config`() {
        val bufferDir = File("/tmp/otel-signals")
        otelConfig.diskBuffering {
            enabled(true)
            maxCacheSize = 5 * 1024 * 1024
            maxCacheFileSize = 512 * 1024
            maxFileAgeForWrite = 1.minutes
            minFileAgeForRead = 2.minutes
            maxFileAgeForRead = 1.days
            exportPeriod = 30.seconds
            signalsBufferDir = bufferDir
        }

        assertThat(otelConfig.rumConfig.getDiskBufferingConfig())
            .isEqualTo(
                DiskBufferingConfig.create(
                    enabled = true,
                    maxCacheSize = 5 * 1024 * 1024,
                    maxFileAgeForWriteMillis = 60_000,
                    minFileAgeForReadMillis = 120_000,
                    maxFileAgeForReadMillis = 86_400_000,
                    maxCacheFileSize = 512 * 1024,
                    signalsBufferDir = bufferDir,
                    exportPeriodMillis = 30_000,
                ),
            )
    }

    @Test
    fun `does not validate intermediate state within a block`() {
        otelConfig.diskBuffering {
            maxFileAgeForWrite = 60.seconds
            minFileAgeForRead = 90.seconds
        }

        val config = otelConfig.rumConfig.getDiskBufferingConfig()
        assertThat(config.maxFileAgeForWriteMillis).isEqualTo(60_000)
        assertThat(config.minFileAgeForReadMillis).isEqualTo(90_000)
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
