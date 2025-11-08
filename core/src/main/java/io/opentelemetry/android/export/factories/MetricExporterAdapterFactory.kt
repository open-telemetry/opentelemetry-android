/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.factories

import io.opentelemetry.android.export.MetricExporterAdapter
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.metrics.export.MetricExporter

/**
 * Factory for creating [MetricExporterAdapter] instances.
 */
internal interface MetricExporterAdapterFactory {
    /**
     * Creates a metric exporter adapter with the given exporter and session provider.
     * @param exporter the underlying metric exporter to wrap.
     * @param sessionProvider the session provider for retrieving session identifiers.
     * @return a newly created metric exporter adapter.
     */
    fun createMetricExporterAdapter(
        exporter: MetricExporter,
        sessionProvider: SessionProvider,
    ): MetricExporterAdapter
}
