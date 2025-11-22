/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.factories

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.Data

/**
 * Factory for creating metric [Data] instances with injected attributes.
 */
internal interface MetricDataTypeFactory {
    /**
     * Creates a new metric Data instance with attributes injected into all points.
     *
     * @param data the original metric data to copy.
     * @param attributesToInject the session attributes to inject into all points.
     * @return a newly created [Data] with injected attributes, or the original if type is unknown.
     */
    fun createDataWithAttributes(
        data: Data<*>,
        attributesToInject: Attributes,
    ): Data<*>
}
