/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.models

import io.opentelemetry.android.export.factories.SessionMetricDataTypeFactory
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData

/**
 * An [ExponentialHistogramData] implementation that wraps another [ExponentialHistogramData]
 * and provides points with session attributes injected.
 *
 * This class delegates most method calls to the underlying [ExponentialHistogramData] except for
 * [getPoints], which returns [ExponentialHistogramPointData] with session attributes merged in.
 * All original exponential histogram characteristics including aggregation temporality are preserved.
 *
 * Exponential histograms provide more efficient storage for histogram data by using exponentially
 * sized buckets. This wrapper preserves that efficiency while adding session correlation.
 *
 * @property originalData the original ExponentialHistogramData to wrap and delegate to.
 * @property pointsWithSession the collection of ExponentialHistogramPointData with session attributes injected.
 * @see SessionMetricDataTypeFactory
 */
internal class ExponentialHistogramWithSessionData(
    private val originalData: ExponentialHistogramData,
    private val pointsWithSession: Collection<ExponentialHistogramPointData>,
) : ExponentialHistogramData {
    override fun getPoints(): Collection<ExponentialHistogramPointData> = pointsWithSession

    override fun getAggregationTemporality(): AggregationTemporality = originalData.aggregationTemporality
}
