/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.metrics

import io.opentelemetry.semconv.ServiceAttributes
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes
import java.util.Collections.unmodifiableSet

class MetricsConfig {
    private val metricResourceKeysToInclude: MutableSet<String> = HashSet()
    private val metricAttributesToInclude: MutableSet<String> = HashSet()

    fun includeMetricResourceAttributes(vararg keys: String): MetricsConfig {
        metricResourceKeysToInclude.addAll(listOf(*keys))
        return this
    }

    fun includeMetricAttributeKeys(vararg keys: String): MetricsConfig {
        metricAttributesToInclude.addAll(listOf(*keys))
        return this
    }

    fun getMetricResourceKeysToInclude(): Set<String> = unmodifiableSet(metricResourceKeysToInclude)

    fun getMetricAttributesToInclude(): Set<String> = unmodifiableSet(metricAttributesToInclude)

    fun hasMetricResourceKeysToInclude(): Boolean = metricResourceKeysToInclude.isEmpty()

    fun isEmpty(): Boolean = metricAttributesToInclude.isEmpty() && metricResourceKeysToInclude.isEmpty()

    companion object {
        fun withDefaults(): MetricsConfig =
            MetricsConfig()
//                .includeMetricAttributeKeys("tbd")
                .includeMetricResourceAttributes(
                    ServiceAttributes.SERVICE_NAME.key,
                    ServiceAttributes.SERVICE_VERSION.key,
                    OsIncubatingAttributes.OS_NAME.key,
                    OsIncubatingAttributes.OS_TYPE.key,
                    OsIncubatingAttributes.OS_VERSION.key,
                )
    }
}
