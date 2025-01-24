/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector
import io.opentelemetry.sdk.metrics.export.MetricExporter

/**
 * An in-memory buffer delegating metric exporter that buffers metrics in memory until a delegate is set.
 * Once a delegate is set, the buffered metrics are exported to the delegate.
 *
 * The buffer size is set to 5,000 metrics by default. If the buffer is full, the exporter will drop new metrics.
 */
internal class BufferDelegatingMetricExporter(
    maxBufferedMetrics: Int = 5_000,
) : MetricExporter {
    private val delegatingExporter =
        DelegatingExporter<MetricExporter, MetricData>(
            doExport = MetricExporter::export,
            doFlush = MetricExporter::flush,
            doShutdown = MetricExporter::shutdown,
            maxBufferedData = maxBufferedMetrics,
            logType = "metrics",
        )
    private var aggregationTemporalitySelector: AggregationTemporalitySelector = AggregationTemporalitySelector.alwaysCumulative()

    fun setDelegate(delegate: MetricExporter) {
        delegatingExporter.setDelegate(delegate)
        aggregationTemporalitySelector = delegate
    }

    override fun getAggregationTemporality(instrumentType: InstrumentType): AggregationTemporality =
        aggregationTemporalitySelector.getAggregationTemporality(instrumentType)

    override fun export(metrics: Collection<MetricData>): CompletableResultCode = delegatingExporter.export(metrics)

    override fun flush(): CompletableResultCode = delegatingExporter.flush()

    override fun shutdown(): CompletableResultCode = delegatingExporter.shutdown()
}
