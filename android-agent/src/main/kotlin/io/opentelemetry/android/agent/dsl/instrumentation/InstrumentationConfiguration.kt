/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader

/**
 * Type-safe config DSL that controls how instrumentation should behave.
 */
@OpenTelemetryDslMarker
class InstrumentationConfiguration internal constructor(
    config: OtelRumConfig,
    private val instrumentationLoader: AndroidInstrumentationLoader,
) {
    private val activity: ActivityLifecycleConfiguration by lazy {
        ActivityLifecycleConfiguration(config, instrumentationLoader)
    }
    private val fragment: FragmentLifecycleConfiguration by lazy {
        FragmentLifecycleConfiguration(config, instrumentationLoader)
    }
    private val anr: AnrReporterConfiguration by lazy { AnrReporterConfiguration(config, instrumentationLoader) }
    private val crash: CrashReporterConfiguration by lazy { CrashReporterConfiguration(config, instrumentationLoader) }
    private val networkMonitoring: NetworkMonitoringConfiguration by lazy {
        NetworkMonitoringConfiguration(config, instrumentationLoader)
    }
    private val slowRendering: SlowRenderingReporterConfiguration by lazy {
        SlowRenderingReporterConfiguration(config, instrumentationLoader)
    }

    private val screenOrientation: ScreenOrientationConfiguration by lazy {
        ScreenOrientationConfiguration(config, instrumentationLoader)
    }

    /**
     * Configures activity lifecycle instrumentation.
     */
    fun activity(configure: ActivityLifecycleConfiguration.() -> Unit) {
        activity.configure()
    }

    /**
     * Configures fragment lifecycle instrumentation.
     */
    fun fragment(configure: FragmentLifecycleConfiguration.() -> Unit) {
        fragment.configure()
    }

    /**
     * Configures ANR detection instrumentation.
     */
    fun anrReporter(configure: AnrReporterConfiguration.() -> Unit) {
        anr.configure()
    }

    /**
     * Configures crash reporting instrumentation.
     */
    fun crashReporter(configure: CrashReporterConfiguration.() -> Unit) {
        crash.configure()
    }

    /**
     * Configures network change instrumentation.
     */
    fun networkMonitoring(configure: NetworkMonitoringConfiguration.() -> Unit) {
        networkMonitoring.configure()
    }

    /**
     * Configures slow render event instrumentation.
     */
    fun slowRenderingReporter(configure: SlowRenderingReporterConfiguration.() -> Unit) {
        slowRendering.configure()
    }

    /**
     * Configures screen orientation event instrumentation
     */
    fun screenOrientation(configure: ScreenOrientationConfiguration.() -> Unit) {
        screenOrientation.configure()
    }
}
