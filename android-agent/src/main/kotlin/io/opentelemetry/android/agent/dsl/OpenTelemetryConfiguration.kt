/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.dsl.instrumentation.InstrumentationConfiguration
import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.api.common.Attributes

/**
 * Type-safe config DSL that controls how OpenTelemetry should behave.
 */
@OptIn(Incubating::class)
@OpenTelemetryDslMarker
class OpenTelemetryConfiguration internal constructor(
    internal val rumConfig: OtelRumConfig = OtelRumConfig(),
) {
    internal val exportConfig = HttpExportConfiguration()
    internal val sessionConfig = SessionConfiguration()
    internal val diskBufferingConfig = DiskBufferingConfigurationSpec()
    internal val instrumentations = InstrumentationConfiguration(rumConfig)

    init {
        diskBuffering {}
    }

    /**
     * Configures how OpenTelemetry should export telemetry over HTTP.
     */
    fun httpExport(action: HttpExportConfiguration.() -> Unit) {
        exportConfig.action()
    }

    /**
     * Configures individual instrumentations.
     */
    fun instrumentations(action: InstrumentationConfiguration.() -> Unit) {
        instrumentations.action()
    }

    /**
     * Configures session behavior.
     */
    fun session(action: SessionConfiguration.() -> Unit) {
        sessionConfig.action()
    }

    /**
     * Configures attributes that are used globally.
     */
    fun globalAttributes(action: () -> Attributes) {
        rumConfig.setGlobalAttributes(action())
    }

    /**
     * Configures disk buffering behavior of exported telemetry.
     */
    fun diskBuffering(action: DiskBufferingConfigurationSpec.() -> Unit) {
        diskBufferingConfig.action()
        rumConfig.setDiskBufferingConfig(DiskBufferingConfig.create(enabled = diskBufferingConfig.enabled))
    }
}
