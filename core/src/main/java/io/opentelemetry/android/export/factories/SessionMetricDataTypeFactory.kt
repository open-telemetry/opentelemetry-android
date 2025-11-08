/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.factories

import io.opentelemetry.android.export.models.ExponentialHistogramWithSessionData
import io.opentelemetry.android.export.models.GaugeWithSessionData
import io.opentelemetry.android.export.models.HistogramWithSessionData
import io.opentelemetry.android.export.models.SumWithSessionData
import io.opentelemetry.android.export.models.SummaryWithSessionData
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.Data
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData
import io.opentelemetry.sdk.metrics.data.GaugeData
import io.opentelemetry.sdk.metrics.data.HistogramData
import io.opentelemetry.sdk.metrics.data.HistogramPointData
import io.opentelemetry.sdk.metrics.data.PointData
import io.opentelemetry.sdk.metrics.data.SumData
import io.opentelemetry.sdk.metrics.data.SummaryData
import io.opentelemetry.sdk.metrics.data.SummaryPointData

/**
 * A [MetricDataTypeFactory] that creates metric Data instances with session attributes injected.
 *
 * This factory creates new Data objects (SumData, GaugeData, HistogramData, etc.) that wrap
 * the original data and inject session attributes into all data points. The original data
 * remains unchanged.
 */
internal open class SessionMetricDataTypeFactory(
    private val pointDataFactory: PointDataFactory = SessionPointDataFactory(),
) : MetricDataTypeFactory {
    /**
     * Creates a new metric Data with session attributes injected into all points.
     *
     * The factory examines the type of the data and creates the appropriate
     * data type (SumWithSessionData, GaugeWithSessionData, etc.) with injected attributes.
     *
     * @param data the original metric data to copy.
     * @param attributesToInject the session attributes to inject into all points.
     * @return a newly created [Data] with session attributes in all points.
     */
    @Suppress("UNCHECKED_CAST")
    override fun createDataWithAttributes(
        data: Data<*>,
        attributesToInject: Attributes,
    ): Data<*> =
        when (data) {
            is SumData<*> -> createSumDataWithAttributes(data, attributesToInject)
            is GaugeData<*> -> createGaugeDataWithAttributes(data, attributesToInject)
            is HistogramData -> createHistogramDataWithAttributes(data, attributesToInject)
            is ExponentialHistogramData -> createExponentialHistogramDataWithAttributes(data, attributesToInject)
            is SummaryData -> createSummaryDataWithAttributes(data, attributesToInject)
            else -> data
        }

    @Suppress("UNCHECKED_CAST")
    private fun createSumDataWithAttributes(
        sumData: SumData<*>,
        attributesToInject: Attributes,
    ): SumData<*> {
        val modifiedPoints =
            pointDataFactory.createPointsWithAttributes(
                sumData.points as Collection<PointData>,
                attributesToInject,
            )
        return SumWithSessionData(sumData, modifiedPoints as Collection<Nothing>)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createGaugeDataWithAttributes(
        gaugeData: GaugeData<*>,
        attributesToInject: Attributes,
    ): GaugeData<*> {
        val modifiedPoints =
            pointDataFactory.createPointsWithAttributes(
                gaugeData.points as Collection<PointData>,
                attributesToInject,
            )
        return GaugeWithSessionData(modifiedPoints as Collection<Nothing>)
    }

    private fun createHistogramDataWithAttributes(
        histogramData: HistogramData,
        attributesToInject: Attributes,
    ): HistogramData {
        val modifiedPoints =
            pointDataFactory.createPointsWithAttributes(
                histogramData.points,
                attributesToInject,
            )
        return HistogramWithSessionData(histogramData, modifiedPoints)
    }

    private fun createExponentialHistogramDataWithAttributes(
        exponentialHistogramData: ExponentialHistogramData,
        attributesToInject: Attributes,
    ): ExponentialHistogramData {
        val modifiedPoints =
            pointDataFactory.createPointsWithAttributes(
                exponentialHistogramData.points,
                attributesToInject,
            )
        return ExponentialHistogramWithSessionData(exponentialHistogramData, modifiedPoints)
    }

    private fun createSummaryDataWithAttributes(
        summaryData: SummaryData,
        attributesToInject: Attributes,
    ): SummaryData {
        val modifiedPoints =
            pointDataFactory.createPointsWithAttributes(
                summaryData.points,
                attributesToInject,
            )
        return SummaryWithSessionData(modifiedPoints)
    }
}
