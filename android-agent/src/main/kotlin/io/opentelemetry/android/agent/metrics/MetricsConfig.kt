package io.opentelemetry.android.agent.metrics

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

    fun getMetricResourceKeysToInclude(): Set<String> {
        return unmodifiableSet(metricResourceKeysToInclude)
    }

    fun getMetricAttributesToInclude(): Set<String> {
        return unmodifiableSet(metricAttributesToInclude)
    }

    fun hasMetricResourceKeysToInclude(): Boolean {
        return metricResourceKeysToInclude.isEmpty()
    }

    fun isEmpty(): Boolean {
        return metricAttributesToInclude.isEmpty() && metricResourceKeysToInclude.isEmpty();
    }

    companion object {
        fun withDefaults(): MetricsConfig {
            return MetricsConfig()
                .includeMetricAttributeKeys("tbd")
                .includeMetricResourceAttributes("tbd")
        }
    }
}