/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.config.OtelRumConfig

@OpenTelemetryDslMarker
class InstrumentationConfiguration internal constructor(
    config: OtelRumConfig,
) {
    private val activity: ActivityLifecycleConfiguration by lazy {
        ActivityLifecycleConfiguration(
            config,
        )
    }
    private val fragment: FragmentLifecycleConfiguration by lazy {
        FragmentLifecycleConfiguration(
            config,
        )
    }
    private val anr: AnrReporterConfiguration by lazy { AnrReporterConfiguration(config) }
    private val crash: CrashReporterConfiguration by lazy { CrashReporterConfiguration(config) }
    private val networkMonitoring: NetworkMonitoringConfiguration by lazy {
        NetworkMonitoringConfiguration(
            config,
        )
    }
    private val slowRendering: SlowRenderingReporterConfiguration by lazy {
        SlowRenderingReporterConfiguration(
            config,
        )
    }

    private val screenOrientation: ScreenOrientationConfiguration by lazy {
        ScreenOrientationConfiguration(
            config
        )
    }

    fun activity(configure: ActivityLifecycleConfiguration.() -> Unit) {
        activity.configure()
    }

    fun fragment(configure: FragmentLifecycleConfiguration.() -> Unit) {
        fragment.configure()
    }

    fun anrReporter(configure: AnrReporterConfiguration.() -> Unit) {
        anr.configure()
    }

    fun crashReporter(configure: CrashReporterConfiguration.() -> Unit) {
        crash.configure()
    }

    fun networkMonitoring(configure: NetworkMonitoringConfiguration.() -> Unit) {
        networkMonitoring.configure()
    }

    fun slowRenderingReporter(configure: SlowRenderingReporterConfiguration.() -> Unit) {
        slowRendering.configure()
    }

    fun screenOrientation(configure: ScreenOrientationConfiguration.() -> Unit) {
        screenOrientation.configure()
    }
}
