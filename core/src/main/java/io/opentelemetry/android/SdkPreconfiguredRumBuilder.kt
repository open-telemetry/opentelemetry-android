/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.OpenTelemetrySdk

class SdkPreconfiguredRumBuilder internal constructor(
    private val application: Application,
    private val sdk: OpenTelemetrySdk,
    private val sessionProvider: SessionProvider,
    private val config: OtelRumConfig,
) {
    private val instrumentations = mutableListOf<AndroidInstrumentation>()

    /**
     * Adds an instrumentation to be applied as a part of the [build] method call.
     *
     * @return `this`
     */
    fun addInstrumentation(instrumentation: AndroidInstrumentation): SdkPreconfiguredRumBuilder {
        instrumentations.add(instrumentation)
        return this
    }

    /**
     * Creates a new instance of [OpenTelemetryRum] with the settings of this [ ].
     *
     *
     * This method uses a preconfigured OpenTelemetry SDK and install built-in system
     * instrumentations in the passed Android [Application].
     *
     * @return A new [OpenTelemetryRum] instance.
     */
    fun build(): OpenTelemetryRum {
        val ctx = InstallationContext(application, sdk, sessionProvider)
        val enabledInstrumentations = getEnabledInstrumentations()
        val onShutdown: () -> Unit = {
            for (instrumentation in enabledInstrumentations) {
                instrumentation.uninstall(ctx)
            }
            sdk.shutdown()
        }
        val openTelemetryRum = OpenTelemetryRumImpl(sdk, sessionProvider, onShutdown)

        // Install instrumentations
        for (instrumentation in enabledInstrumentations) {
            instrumentation.install(ctx)
        }

        return openTelemetryRum
    }

    private fun getEnabledInstrumentations(): List<AndroidInstrumentation> =
        getInstrumentations().filter { inst -> !config.isSuppressed(inst.name) }

    private fun getInstrumentations(): List<AndroidInstrumentation> {
        if (config.shouldDiscoverInstrumentations()) {
            instrumentations.addAll(AndroidInstrumentationLoader.get().getAll())
        }

        return instrumentations
    }
}
