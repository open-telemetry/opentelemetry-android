package io.opentelemetry.android.agent.metrics

import java.util.Collections.unmodifiableSet

class MetricsConfig {

    private val metricResourceKeysToOmit: MutableSet<String> = HashSet()
    private val metricAttributesToOmit: MutableSet<String> = HashSet()

    fun omitMetricResourceAttributes(vararg keys: String): MetricsConfig {
        metricResourceKeysToOmit.addAll(listOf(*keys))
        return this
    }

    fun omitMetricAttributeKeys(vararg keys: String): MetricsConfig {
        metricAttributesToOmit.addAll(listOf(*keys))
        return this
    }

    fun getMetricResourceKeysToOmit(): Set<String> {
        return unmodifiableSet(metricResourceKeysToOmit)
    }

    fun getMetricAttributesToOmit(): Set<String> {
        return unmodifiableSet(metricAttributesToOmit)
    }

    fun hasMetricResourceKeysToOmit(): Boolean {
        return metricResourceKeysToOmit.isEmpty()
    }

    fun isEmpty(): Boolean {
        return metricAttributesToOmit.isEmpty() && metricResourceKeysToOmit.isEmpty();
    }

    companion object {
        fun withDefaults(): MetricsConfig {
            return MetricsConfig()
                .omitMetricAttributeKeys("tbd")
                .omitMetricResourceAttributes("tbd")
        }
    }
}