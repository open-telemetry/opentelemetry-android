/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.initialization

import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.ServiceLoader

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
interface InitializationEvents {
    fun sdkInitializationStarted()

    fun recordConfiguration(config: OtelRumConfig)

    fun currentNetworkProviderInitialized()

    fun networkMonitorInitialized()

    fun anrMonitorInitialized()

    fun slowRenderingDetectorInitialized()

    fun crashReportingInitialized()

    fun spanExporterInitialized(spanExporter: SpanExporter)

    companion object {
        private var instance: InitializationEvents? = null

        @JvmStatic
        fun get(): InitializationEvents {
            if (instance == null) {
                val initializationEvents = loadInitializationEvents().firstOrNull()
                if (initializationEvents != null) {
                    set(initializationEvents)
                } else {
                    set(NO_OP)
                }
            }

            return instance ?: error("instance can't be null here")
        }

        @JvmStatic
        fun set(initializationEvents: InitializationEvents) {
            if (instance == null) {
                instance = initializationEvents
            }
        }

        @JvmStatic
        fun resetForTest() {
            instance = null
        }

        val NO_OP: InitializationEvents =
            object : InitializationEvents {
                override fun sdkInitializationStarted() {}

                override fun recordConfiguration(config: OtelRumConfig) {}

                override fun currentNetworkProviderInitialized() {}

                override fun networkMonitorInitialized() {}

                override fun anrMonitorInitialized() {}

                override fun slowRenderingDetectorInitialized() {}

                override fun crashReportingInitialized() {}

                override fun spanExporterInitialized(spanExporter: SpanExporter) {}
            }
    }
}

/**
 * Loads the [InitializationEvents] implementations declared via SPI. See the R8 note on
 * `AndroidInstrumentationLoaderImpl.loadInstrumentations` before changing the shape of this
 * function.
 */
private fun loadInitializationEvents(): List<InitializationEvents> {
    val events = mutableListOf<InitializationEvents>()
    for (event in ServiceLoader.load(InitializationEvents::class.java, InitializationEvents::class.java.classLoader)) {
        events.add(event)
    }
    return events
}
