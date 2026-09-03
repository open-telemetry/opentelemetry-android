/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.dsl.instrumentation.CanBeEnabledAndDisabled
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DEFAULT_MAX_CACHE_SIZE
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.android.features.diskbuffering.MAX_CACHE_FILE_SIZE
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Type-safe config DSL that controls how disk buffering of exported telemetry should behave.
 */
@OpenTelemetryDslMarker
class DiskBufferingConfigurationSpec internal constructor(
    private val rumConfig: OtelRumConfig,
) : CanBeEnabledAndDisabled {
    internal var enabled: Boolean = true

    /**
     * The maximum amount of disk space, in bytes, that buffered telemetry may occupy.
     */
    var maxCacheSize: Int = DEFAULT_MAX_CACHE_SIZE

    /**
     * The maximum size, in bytes, of a single buffer file.
     */
    var maxCacheFileSize: Int = MAX_CACHE_FILE_SIZE

    /**
     * The age at which a buffer file stops being appended to and a new one is started. Must be
     * shorter than [minFileAgeForRead].
     */
    var maxFileAgeForWrite: Duration = 30.seconds

    /**
     * The minimum age a buffer file must reach before its eligible for
     * export.
     */
    var minFileAgeForRead: Duration = 33.seconds

    /**
     * The age at which a buffer file is considered stale and is discarded without being exported.
     */
    var maxFileAgeForRead: Duration = 18.hours

    /**
     * How often buffered telemetry is read back from disk and exported. Must be greater than zero.
     */
    var exportPeriod: Duration = 10.seconds

    /**
     * The directory in which buffered telemetry is stored until it is exported. By default
     * the application's cache directory is used.
     */
    var signalsBufferDir: File? = null

    init {
        applyToRumConfig()
    }

    override fun enabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /**
     * Builds a complete [DiskBufferingConfig] from the current state
     */
    internal fun applyToRumConfig() {
        rumConfig.setDiskBufferingConfig(
            DiskBufferingConfig.create(
                enabled = enabled,
                maxCacheSize = maxCacheSize,
                maxFileAgeForWriteMillis = maxFileAgeForWrite.inWholeMilliseconds,
                minFileAgeForReadMillis = minFileAgeForRead.inWholeMilliseconds,
                maxFileAgeForReadMillis = maxFileAgeForRead.inWholeMilliseconds,
                maxCacheFileSize = maxCacheFileSize,
                signalsBufferDir = signalsBufferDir,
                exportPeriodMillis = exportPeriod.inWholeMilliseconds,
            ),
        )
    }
}
