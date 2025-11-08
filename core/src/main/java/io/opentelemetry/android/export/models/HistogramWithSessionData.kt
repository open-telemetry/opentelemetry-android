/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.models

import io.opentelemetry.android.export.factories.SessionMetricDataTypeFactory
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.HistogramData
import io.opentelemetry.sdk.metrics.data.HistogramPointData

/**
 * A [HistogramData] implementation that wraps another [HistogramData] and provides points with
 * session attributes injected.
 *
 * This class delegates most method calls to the underlying [HistogramData] except for [getPoints],
 * which returns [HistogramPointData] with session attributes merged in.
 * All original histogram characteristics including aggregation temporality are preserved.
 *
 * @property originalData the original HistogramData to wrap and delegate to.
 * @property pointsWithSession the collection of HistogramPointData with session attributes injected.
 * @see SessionMetricDataTypeFactory
 */
internal class HistogramWithSessionData(
    private val originalData: HistogramData,
    private val pointsWithSession: Collection<HistogramPointData>,
) : HistogramData {
    override fun getPoints(): Collection<HistogramPointData> = pointsWithSession

    override fun getAggregationTemporality(): AggregationTemporality = originalData.aggregationTemporality
}
