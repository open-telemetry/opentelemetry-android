/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.android.export.factories.MetricDataFactory
import io.opentelemetry.android.export.factories.SessionMetricDataFactory
import io.opentelemetry.android.ktx.setSessionIdentifiersWith
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter

/**
 * Implementation of [MetricExporterAdapter] that injects session identifiers.
 *
 * This adapter creates new metric data with session attributes injected into all points
 * using a [MetricDataFactory] before delegating to the underlying exporter. The session
 * attributes are added to each metric point, ensuring proper correlation of metrics with
 * user sessions. This occurs before metrics are written to disk (if disk buffering is
 * enabled) or sent over the network.
 *
 * @param exporter the underlying metric exporter.
 * @param sessionProvider the session provider to retrieve session identifiers from.
 * @param metricDataFactory the factory to create new metric data with injected attributes.
 */
internal class SessionMetricExporterAdapter(
    private val exporter: MetricExporter,
    private val sessionProvider: SessionProvider,
    private val metricDataFactory: MetricDataFactory = SessionMetricDataFactory(),
) : MetricExporterAdapter {
    override fun export(metrics: Collection<MetricData>): CompletableResultCode {
        // Build session attributes using extension function
        val sessionAttributes = buildSessionAttributes()

        // If no session attributes, pass through unchanged
        if (sessionAttributes.isEmpty) {
            return exporter.export(metrics)
        }

        // Create new MetricData instances with session attributes using factory
        val modifiedMetrics =
            metrics.map { metricData ->
                metricDataFactory.createMetricDataWithAttributes(metricData, sessionAttributes)
            }

        return exporter.export(modifiedMetrics)
    }

    private fun buildSessionAttributes(): Attributes =
        Attributes
            .builder()
            .setSessionIdentifiersWith(sessionProvider)
            .build()

    override fun getAggregationTemporality(instrumentType: InstrumentType): AggregationTemporality =
        exporter.getAggregationTemporality(instrumentType)

    override fun flush(): CompletableResultCode = exporter.flush()

    override fun shutdown(): CompletableResultCode = exporter.shutdown()
}
