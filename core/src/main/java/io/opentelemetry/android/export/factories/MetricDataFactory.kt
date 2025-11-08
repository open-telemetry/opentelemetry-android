/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.factories

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.MetricData

/**
 * Factory for creating [MetricData] instances with injected attributes.
 */
internal interface MetricDataFactory {
    /**
     * Creates a new [MetricData] instance with additional attributes injected into all points.
     *
     * @param metricData the original metric data to copy.
     * @param attributesToInject the attributes to inject into all points.
     * @return a newly created [MetricData] with attributes injected into its points.
     */
    fun createMetricDataWithAttributes(
        metricData: MetricData,
        attributesToInject: Attributes,
    ): MetricData
}
