/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.agent.dsl.OpenTelemetryDslMarker
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.network.NetworkAttributesExtractor
import io.opentelemetry.android.instrumentation.network.NetworkChangeInstrumentation

/**
 * Type-safe config DSL that controls how network request instrumentation should behave.
 */
@OpenTelemetryDslMarker
class NetworkMonitoringConfiguration internal constructor(
    private val config: OtelRumConfig,
) : CanBeEnabledAndDisabled {
    private val networkInstrumentation: NetworkChangeInstrumentation by lazy {
        AndroidInstrumentationLoader.getInstrumentation(
            NetworkChangeInstrumentation::class.java,
        )
    }

    /**
     * Supplies an attributes extractor, which can be used to customise the attributes on
     * the network telemetry.
     */
    fun addAttributesExtractor(value: NetworkAttributesExtractor) {
        networkInstrumentation.addAttributesExtractor(value)
    }

    override fun enabled(enabled: Boolean) {
        if (enabled) {
            config.allowInstrumentation(networkInstrumentation.name)
        } else {
            config.suppressInstrumentation(networkInstrumentation.name)
        }
    }
}
