/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.factories

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.DoublePointData
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData
import io.opentelemetry.sdk.metrics.data.HistogramPointData
import io.opentelemetry.sdk.metrics.data.LongPointData
import io.opentelemetry.sdk.metrics.data.PointData
import io.opentelemetry.sdk.metrics.data.SummaryPointData

/**
 * Factory for creating [PointData] instances with injected attributes.
 */
internal interface PointDataFactory {
    /**
     * Creates a collection of point data instances with attributes injected.
     *
     * @param T the type of point data.
     * @param points the original points to process.
     * @param attributesToInject the attributes to inject into each point.
     * @return a collection of newly created points with injected attributes.
     */
    fun <T : PointData> createPointsWithAttributes(
        points: Collection<T>,
        attributesToInject: Attributes,
    ): Collection<T>

    /**
     * Creates a single point data instance with attributes injected.
     *
     * @param T the type of point data.
     * @param pointData the original point to process.
     * @param attributesToInject the attributes to inject into the point.
     * @return a newly created point with injected attributes.
     */
    fun <T : PointData> createPointDataWithAttributes(
        pointData: T,
        attributesToInject: Attributes,
    ): T = createPointsWithAttributes(listOf(pointData), attributesToInject).first()
}
