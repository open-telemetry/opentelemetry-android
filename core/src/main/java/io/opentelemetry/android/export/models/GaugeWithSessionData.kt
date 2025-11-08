/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.models

import io.opentelemetry.android.export.factories.SessionMetricDataTypeFactory
import io.opentelemetry.sdk.metrics.data.GaugeData
import io.opentelemetry.sdk.metrics.data.PointData

/**
 * A [GaugeData] implementation that provides points with session attributes injected.
 *
 * This class returns points with session attributes merged in. Gauge data represents the current
 * value at a point in time and this wrapper preserves that semantic while adding session correlation.
 *
 * @property pointsWithSession the collection of points with session attributes injected.
 * @param T the type of [PointData] contained in the gauge (typically [io.opentelemetry.sdk.metrics.data.LongPointData]
 *     or [io.opentelemetry.sdk.metrics.data.DoublePointData]).
 * @see SessionMetricDataTypeFactory
 */
internal class GaugeWithSessionData<T : PointData>(
    private val pointsWithSession: Collection<T>,
) : GaugeData<T> {
    override fun getPoints(): Collection<T> = pointsWithSession
}
