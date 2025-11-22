/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.models

import io.opentelemetry.android.export.factories.SessionPointDataFactory
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramBuckets
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData

/**
 * An [ExponentialHistogramPointData] implementation that wraps another
 * [ExponentialHistogramPointData] and injects additional session attributes into the
 * point's attributes.
 *
 * This class delegates all method calls to the underlying [ExponentialHistogramPointData]
 * except for [getAttributes], which merges the original attributes with the injected session
 * attributes. All original exponential histogram data including timestamps, counts, sums, scale,
 * zero count, buckets, min/max values, and exemplars are preserved.
 *
 * @property originalData the original ExponentialHistogramPointData to wrap.
 * @property attributesToInject the session attributes to add to the point's attributes.
 * @see SessionPointDataFactory
 */
internal class ExponentialHistogramPointWithSessionData(
    private val originalData: ExponentialHistogramPointData,
    private val attributesToInject: Attributes,
) : ExponentialHistogramPointData {
    override fun getAttributes(): Attributes =
        originalData.attributes
            .toBuilder()
            .putAll(attributesToInject)
            .build()

    override fun getStartEpochNanos(): Long = originalData.startEpochNanos

    override fun getEpochNanos(): Long = originalData.epochNanos

    override fun getCount(): Long = originalData.count

    override fun getSum(): Double = originalData.sum

    override fun getScale(): Int = originalData.scale

    override fun getZeroCount(): Long = originalData.zeroCount

    override fun hasMin(): Boolean = originalData.hasMin()

    override fun getMin(): Double = originalData.min

    override fun hasMax(): Boolean = originalData.hasMax()

    override fun getMax(): Double = originalData.max

    override fun getPositiveBuckets(): ExponentialHistogramBuckets = originalData.positiveBuckets

    override fun getNegativeBuckets(): ExponentialHistogramBuckets = originalData.negativeBuckets

    override fun getExemplars(): List<DoubleExemplarData> = originalData.exemplars
}
