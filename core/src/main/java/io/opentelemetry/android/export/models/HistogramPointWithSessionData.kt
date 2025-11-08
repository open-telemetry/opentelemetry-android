/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.models

import io.opentelemetry.android.export.factories.SessionPointDataFactory
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData
import io.opentelemetry.sdk.metrics.data.HistogramPointData

/**
 * A [HistogramPointData] implementation that wraps another [HistogramPointData] and injects
 * additional session attributes into the point's attributes.
 *
 * This class delegates all method calls to the underlying [HistogramPointData] except for
 * [getAttributes], which merges the original attributes with the injected session attributes.
 * All original histogram data including timestamps, counts, sums, boundaries, min/max values,
 * and exemplars are preserved.
 *
 * @property originalData the original HistogramPointData to wrap and delegate to.
 * @property attributesToInject the session attributes to add to the point's attributes.
 * @see SessionPointDataFactory
 */
internal class HistogramPointWithSessionData(
    private val originalData: HistogramPointData,
    private val attributesToInject: Attributes,
) : HistogramPointData {
    override fun getAttributes(): Attributes =
        originalData.attributes
            .toBuilder()
            .putAll(attributesToInject)
            .build()

    override fun getStartEpochNanos(): Long = originalData.startEpochNanos

    override fun getEpochNanos(): Long = originalData.epochNanos

    override fun getSum(): Double = originalData.sum

    override fun getCount(): Long = originalData.count

    override fun hasMin(): Boolean = originalData.hasMin()

    override fun getMin(): Double = originalData.min

    override fun hasMax(): Boolean = originalData.hasMax()

    override fun getMax(): Double = originalData.max

    override fun getBoundaries(): List<Double> = originalData.boundaries

    override fun getCounts(): List<Long> = originalData.counts

    override fun getExemplars(): List<DoubleExemplarData> = originalData.exemplars
}
