/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.factories

import io.opentelemetry.android.export.models.DoublePointWithSessionData
import io.opentelemetry.android.export.models.ExponentialHistogramPointWithSessionData
import io.opentelemetry.android.export.models.HistogramPointWithSessionData
import io.opentelemetry.android.export.models.LongPointWithSessionData
import io.opentelemetry.android.export.models.SummaryPointWithSessionData
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.DoublePointData
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData
import io.opentelemetry.sdk.metrics.data.HistogramPointData
import io.opentelemetry.sdk.metrics.data.LongPointData
import io.opentelemetry.sdk.metrics.data.PointData
import io.opentelemetry.sdk.metrics.data.SummaryPointData

/**
 * A [PointDataFactory] that creates point data instances with session attributes injected.
 *
 * This factory creates new point data objects by wrapping the original points and injecting
 * session attributes. The original point data remains unchanged.
 */
internal open class SessionPointDataFactory : PointDataFactory {
    /**
     * Creates a collection of point data with session attributes injected.
     *
     * The factory examines the type of each point and creates the appropriate
     * point data type with injected attributes.
     *
     * @param T the type of point data.
     * @param points the original points to process.
     * @param attributesToInject the session attributes to inject into each point.
     * @return a collection of newly created points with session attributes.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : PointData> createPointsWithAttributes(
        points: Collection<T>,
        attributesToInject: Attributes,
    ): Collection<T> =
        points.map { point ->
            when (point) {
                is LongPointData -> LongPointWithSessionData(point, attributesToInject) as T
                is DoublePointData -> DoublePointWithSessionData(point, attributesToInject) as T
                is HistogramPointData -> HistogramPointWithSessionData(point, attributesToInject) as T
                is ExponentialHistogramPointData -> ExponentialHistogramPointWithSessionData(point, attributesToInject) as T
                is SummaryPointData -> SummaryPointWithSessionData(point, attributesToInject) as T
                else -> point
            }
        }
}
