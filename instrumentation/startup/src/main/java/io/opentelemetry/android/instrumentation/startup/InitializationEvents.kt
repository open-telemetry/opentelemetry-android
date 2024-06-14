/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import io.opentelemetry.sdk.trace.export.SpanExporter

interface InitializationEvents {
    fun sdkInitializationStarted()

    fun recordConfiguration(config: Map<String, String>)

    fun currentNetworkProviderInitialized()

    fun networkMonitorInitialized()

    fun anrMonitorInitialized()

    fun slowRenderingDetectorInitialized()

    fun crashReportingInitialized()

    fun spanExporterInitialized(spanExporter: SpanExporter)

    companion object {
        @JvmField
        val NO_OP: InitializationEvents =
            object : InitializationEvents {
                override fun sdkInitializationStarted() {}

                override fun recordConfiguration(config: Map<String, String>) {}

                override fun currentNetworkProviderInitialized() {}

                override fun networkMonitorInitialized() {}

                override fun anrMonitorInitialized() {}

                override fun slowRenderingDetectorInitialized() {}

                override fun crashReportingInitialized() {}

                override fun spanExporterInitialized(spanExporter: SpanExporter) {}
            }
    }
}
