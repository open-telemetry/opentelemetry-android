/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.dsl.instrumentation.CanBeEnabledAndDisabled
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DEFAULT_MAX_CACHE_SIZE
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import java.io.File
import kotlin.time.Duration
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
     *
     * This budget is split evenly three ways, so spans, logs, and metrics each get a third of
     * the value set here, regardless of which signals the application actually emits.
     */
    var maxCacheSize: Int = DEFAULT_MAX_CACHE_SIZE

    /**
     * How often buffered telemetry is read back from disk and exported. Must be greater than zero.
     */
    var exportPeriod: Duration = 10.seconds

    /**
     * The root directory in which buffered telemetry is stored until it is exported. Each signal
     * type is stored in its own subdirectory of this location, as a directory may only hold one
     * of spans, logs, or metrics. By default the application's cache directory is used.
     */
    var telemetryStorageDir: File? = null

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
                signalsBufferDir = telemetryStorageDir,
                exportPeriodMillis = exportPeriod.inWholeMilliseconds,
            ),
        )
    }
}
