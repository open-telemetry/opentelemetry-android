/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.dsl.instrumentation.InstrumentationConfiguration
import io.opentelemetry.android.config.OtelRumConfig

/**
 * Type-safe config DSL that controls how OpenTelemetry should behave.
 */
@OptIn(Incubating::class)
@OpenTelemetryDslMarker
class OpenTelemetryConfiguration {
    internal val exportConfig: HttpExportConfiguration = HttpExportConfiguration()

    internal val rumConfig: OtelRumConfig = OtelRumConfig()
    internal val sessionConfig: SessionConfiguration = SessionConfiguration()

    /**
     * Configures individual instrumentations.
     */
    internal val instrumentations: InstrumentationConfiguration =
        InstrumentationConfiguration(rumConfig)

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
     * Configures session behavior
     */
    fun session(action: SessionConfiguration.() -> Unit) {
        sessionConfig.action()
    }
}
