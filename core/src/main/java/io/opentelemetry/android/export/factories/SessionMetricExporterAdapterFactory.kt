/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.factories

import io.opentelemetry.android.export.MetricExporterAdapter
import io.opentelemetry.android.export.SessionMetricExporterAdapter
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.metrics.export.MetricExporter

/**
 * A [MetricExporterAdapterFactory] that creates [SessionMetricExporterAdapter] instances.
 */
internal open class SessionMetricExporterAdapterFactory : MetricExporterAdapterFactory {
    /**
     * Creates a [SessionMetricExporterAdapter] with the given exporter and session provider.
     * @param exporter the underlying metric exporter to wrap.
     * @param sessionProvider the session provider for retrieving session identifiers.
     * @return a newly created [SessionMetricExporterAdapter] instance.
     */
    override fun createMetricExporterAdapter(
        exporter: MetricExporter,
        sessionProvider: SessionProvider,
    ): MetricExporterAdapter = SessionMetricExporterAdapter(exporter, sessionProvider)
}
