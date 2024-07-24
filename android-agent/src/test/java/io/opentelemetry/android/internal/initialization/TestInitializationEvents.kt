/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.initialization

import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.sdk.trace.export.SpanExporter

class TestInitializationEvents : InitializationEvents {
    override fun sdkInitializationStarted() {
    }

    override fun recordConfiguration(config: OtelRumConfig) {
    }

    override fun currentNetworkProviderInitialized() {
    }

    override fun networkMonitorInitialized() {
    }

    override fun anrMonitorInitialized() {
    }

    override fun slowRenderingDetectorInitialized() {
    }

    override fun crashReportingInitialized() {
    }

    override fun spanExporterInitialized(spanExporter: SpanExporter) {
    }
}
