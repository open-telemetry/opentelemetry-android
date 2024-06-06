/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import io.opentelemetry.android.instrumentation.common.InstrumentedApplication
import io.opentelemetry.android.internal.services.ServiceManager.Companion.get
import io.opentelemetry.sdk.OpenTelemetrySdk
import java.util.function.Consumer

class SdkPreconfiguredRumBuilder
    @JvmOverloads
    internal constructor(
        private val application: Application,
        private val sdk: OpenTelemetrySdk,
        private val sessionId: SessionId =
            SessionId(
                SessionIdTimeoutHandler(),
            ),
    ) {
        private val instrumentationInstallers: MutableList<Consumer<InstrumentedApplication>> =
            ArrayList()

        /**
         * Adds an instrumentation installer function that will be run on an [ ] instance as a part of the [.build] method call.
         *
         * @return `this`
         */
        fun addInstrumentation(instrumentationInstaller: Consumer<InstrumentedApplication>): SdkPreconfiguredRumBuilder {
            instrumentationInstallers.add(instrumentationInstaller)
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
            // the app state listeners need to be run in the first ActivityLifecycleCallbacks since they
            // might turn off/on additional telemetry depending on whether the app is active or not
            get()
                .getAppLifecycleService()
                .registerListener(sessionId.timeoutHandler)

            val tracer = sdk.getTracer(OpenTelemetryRum::class.java.simpleName)
            sessionId.setSessionIdChangeListener(SessionIdChangeTracer(tracer))

            //        InstrumentedApplication instrumentedApplication =
            //                new InstrumentedApplicationImpl(application, sdk,
            // applicationStateWatcher);
            //        for (Consumer<InstrumentedApplication> installer : instrumentationInstallers) {
            //            installer.accept(instrumentedApplication); TODO to be replaced by calls to
            // AndroidInstrumentation.install
            //        }
            return OpenTelemetryRumImpl(sdk, sessionId)
        }
    }
