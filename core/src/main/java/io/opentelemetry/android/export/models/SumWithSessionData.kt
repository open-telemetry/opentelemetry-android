/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.models

import io.opentelemetry.android.export.factories.SessionMetricDataTypeFactory
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.DoublePointData
import io.opentelemetry.sdk.metrics.data.LongPointData
import io.opentelemetry.sdk.metrics.data.PointData
import io.opentelemetry.sdk.metrics.data.SumData

/**
 * A [SumData] implementation that wraps another [SumData] and provides points with
 * session attributes injected.
 *
 * This class delegates most method calls to the underlying [SumData] except for [getPoints],
 * which returns points with session attributes merged in.
 * All original sum characteristics including monotonicity and aggregation temporality are
 * preserved.
 *
 * @property originalData the original SumData to wrap and delegate to.
 * @property pointsWithSession the collection of points with session attributes injected.
 * @param T the type of [PointData] contained in the sum (typically [LongPointData] or
 * [DoublePointData]).
 * @see SessionMetricDataTypeFactory
 */
internal class SumWithSessionData<T : PointData>(
    private val originalData: SumData<T>,
    private val pointsWithSession: Collection<T>,
) : SumData<T> {
    override fun getPoints(): Collection<T> = pointsWithSession

    override fun isMonotonic(): Boolean = originalData.isMonotonic

    override fun getAggregationTemporality(): AggregationTemporality = originalData.aggregationTemporality
}
