/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.models

import io.opentelemetry.android.export.factories.SessionPointDataFactory
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.LongExemplarData
import io.opentelemetry.sdk.metrics.data.LongPointData

/**
 * A [LongPointData] implementation that wraps another [LongPointData] and injects
 * additional session attributes into the point's attributes.
 *
 * This class delegates all method calls to the underlying [LongPointData] except for
 * [getAttributes], which merges the original attributes with the injected session attributes.
 * All original data including timestamps, values, and exemplars are preserved.
 *
 * @property originalData the original LongPointData to wrap and delegate to.
 * @property attributesToInject the session attributes to add to the point's attributes.
 * @see SessionPointDataFactory
 */
internal class LongPointWithSessionData(
    private val originalData: LongPointData,
    private val attributesToInject: Attributes,
) : LongPointData {
    override fun getStartEpochNanos(): Long = originalData.startEpochNanos

    override fun getEpochNanos(): Long = originalData.epochNanos

    override fun getAttributes(): Attributes =
        originalData.attributes
            .toBuilder()
            .putAll(attributesToInject)
            .build()

    override fun getValue(): Long = originalData.value

    override fun getExemplars(): List<LongExemplarData> = originalData.exemplars
}
