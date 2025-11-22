/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.models

import io.opentelemetry.android.export.factories.MetricDataTypeFactory
import io.opentelemetry.android.export.factories.SessionMetricDataFactory
import io.opentelemetry.android.export.factories.SessionMetricDataTypeFactory
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.InstrumentationScopeInfo
import io.opentelemetry.sdk.metrics.data.Data
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.data.MetricDataType
import io.opentelemetry.sdk.resources.Resource

/**
 * A [MetricData] implementation that wraps another [MetricData] and provides data with
 * session attributes injected into all points.
 *
 * This class delegates all method calls to the underlying [MetricData] except for [getData],
 * which returns [Data] instances with session attributes merged into all points.
 *
 * All original metric metadata including name, description, unit, type, resource, and
 * instrumentation scope are preserved. Only the data points themselves are modified to
 * include additional session attributes.
 *
 * @property originalData the original metric data to wrap and delegate to.
 * @property attributesToInject the session attributes to inject into all points.
 * @property dataTypeFactory the factory to create adapted data types with injected attributes.
 *
 * @see SessionMetricDataFactory
 * @see MetricDataTypeFactory
 */
internal class ModifiedMetricData(
    private val originalData: MetricData,
    attributesToInject: Attributes,
    dataTypeFactory: MetricDataTypeFactory = SessionMetricDataTypeFactory(),
) : MetricData {
    // Create the adapted data once upfront, not on every getData() call
    private val adaptedData: Data<*> = dataTypeFactory.createDataWithAttributes(originalData.data, attributesToInject)

    override fun getName(): String = originalData.name

    override fun getDescription(): String = originalData.description

    override fun getUnit(): String = originalData.unit

    override fun getType(): MetricDataType = originalData.type

    override fun getResource(): Resource = originalData.resource

    override fun getInstrumentationScopeInfo(): InstrumentationScopeInfo = originalData.instrumentationScopeInfo

    override fun getData(): Data<*> {
        // Return the pre-created adapted data
        return adaptedData
    }

    override fun isEmpty(): Boolean = originalData.isEmpty
}
