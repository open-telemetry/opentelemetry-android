/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.factories

import io.opentelemetry.android.export.models.ModifiedMetricData
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.MetricData

/**
 * A [MetricDataFactory] that creates [MetricData] instances with session attributes injected.
 *
 * This factory creates new MetricData objects that wrap the original metric data and inject
 * session attributes into all data points. The original metric data remains unchanged.
 */
internal open class SessionMetricDataFactory : MetricDataFactory {
    /**
     * Creates a new [MetricData] with session attributes injected into all points.
     *
     * @param metricData the original metric data to copy.
     * @param attributesToInject the session attributes to inject into all points.
     * @return a newly created [ModifiedMetricData] with session attributes in all points.
     */
    override fun createMetricDataWithAttributes(
        metricData: MetricData,
        attributesToInject: Attributes,
    ): MetricData = ModifiedMetricData(metricData, attributesToInject)
}
