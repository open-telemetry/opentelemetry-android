/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.models

import io.opentelemetry.android.export.factories.SessionPointDataFactory
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData
import io.opentelemetry.sdk.metrics.data.SummaryPointData
import io.opentelemetry.sdk.metrics.data.ValueAtQuantile

/**
 * A [SummaryPointData] implementation that wraps another [SummaryPointData] and injects
 * additional session attributes into the point's attributes.
 *
 * This class delegates all method calls to the underlying [SummaryPointData] except for
 * [getAttributes], which merges the original attributes with the injected session attributes.
 * All original summary data including timestamps, counts, sums, quantile values, and exemplars
 * are preserved.
 *
 * @property originalData the original SummaryPointData to wrap and delegate to.
 * @property attributesToInject the session attributes to add to the point's attributes.
 * @see SessionPointDataFactory
 */
internal class SummaryPointWithSessionData(
    private val originalData: SummaryPointData,
    private val attributesToInject: Attributes,
) : SummaryPointData {
    override fun getAttributes(): Attributes =
        originalData.attributes
            .toBuilder()
            .putAll(attributesToInject)
            .build()

    override fun getStartEpochNanos(): Long = originalData.startEpochNanos

    override fun getEpochNanos(): Long = originalData.epochNanos

    override fun getValues(): List<ValueAtQuantile> = originalData.values

    override fun getCount(): Long = originalData.count

    override fun getSum(): Double = originalData.sum

    @Suppress("UNCHECKED_CAST")
    override fun getExemplars(): List<DoubleExemplarData> = originalData.exemplars as List<DoubleExemplarData>
}
