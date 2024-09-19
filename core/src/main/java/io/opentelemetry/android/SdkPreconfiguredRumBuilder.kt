/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.OpenTelemetrySdk

class SdkPreconfiguredRumBuilder
    @JvmOverloads
    internal constructor(
        private val application: Application,
        private val sdk: OpenTelemetrySdk,
        private val sessionProvider: SessionProvider,
        private val discoverInstrumentations: Boolean,
        private val serviceManager: ServiceManager,
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
            serviceManager.start()

            val openTelemetryRum = OpenTelemetryRumImpl(sdk, sessionProvider)

            // Install instrumentations
            for (instrumentation in getInstrumentations()) {
                instrumentation.install(application, openTelemetryRum)
            }

            return openTelemetryRum
        }

        private fun getInstrumentations(): List<AndroidInstrumentation> {
            if (discoverInstrumentations) {
                instrumentations.addAll(AndroidInstrumentationLoader.get().getAll())
            }

            return instrumentations
        }
    }
