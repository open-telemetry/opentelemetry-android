/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OtelAndroidClock
import io.opentelemetry.android.agent.dsl.instrumentation.InstrumentationConfiguration
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.resources.ResourceBuilder

/**
 * Type-safe config DSL that controls how OpenTelemetry should behave.
 */
@OptIn(Incubating::class)
@OpenTelemetryDslMarker
class OpenTelemetryConfiguration internal constructor(
    internal val rumConfig: OtelRumConfig = OtelRumConfig(),
    internal val diskBufferingConfig: DiskBufferingConfigurationSpec = DiskBufferingConfigurationSpec(rumConfig),
    var clock: Clock = OtelAndroidClock(),
) {
    internal val exportConfig = HttpExportConfiguration()
    internal var grpcExportConfig: GrpcExportConfiguration? = null
    internal var unifiedExportConfig: ExportConfiguration? = null
    internal val sessionConfig = SessionConfiguration()
    internal val instrumentations = InstrumentationConfiguration(rumConfig)
    internal var resourceAction: ResourceBuilder.() -> Unit = {}

    /**
     * Configures exporting of telemetry over HTTP using [HttpExportConfiguration].
     *
     * This is the default export configuration. If [export] or [grpcExport] is also called,
     * the precedence order is: [export] > [grpcExport] > [httpExport].
     */
    fun httpExport(action: HttpExportConfiguration.() -> Unit) {
        exportConfig.action()
    }

    /**
     * Configures exporting of telemetry over gRPC using [GrpcExportConfiguration].
     *
     * If [export] is also called, it takes precedence over this configuration.
     * This configuration takes precedence over [httpExport].
     */
    fun grpcExport(action: GrpcExportConfiguration.() -> Unit) {
        grpcExportConfig = GrpcExportConfiguration().apply(action)
    }

    /**
     * Configures export settings using a unified DSL that supports both HTTP and gRPC protocols.
     *
     * This is the preferred way to configure telemetry export. Use [ExportConfiguration.protocol]
     * to select the transport (HTTP or gRPC).
     *
     * This configuration takes precedence over both [httpExport] and [grpcExport].
     */
    fun export(action: ExportConfiguration.() -> Unit) {
        unifiedExportConfig = ExportConfiguration().apply(action)
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
    }

    /**
     * Configures the resource attributes that are used globally by acting on a [ResourceBuilder].
     */
    fun resource(action: ResourceBuilder.() -> Unit) {
        resourceAction = action
    }
}
