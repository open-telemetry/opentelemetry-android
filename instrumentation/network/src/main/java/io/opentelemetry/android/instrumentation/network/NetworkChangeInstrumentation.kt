/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network

import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.internal.services.Services.Companion.get
import io.opentelemetry.api.common.AttributesBuilder

/**
 * A tag interface for an extractor that can add attributes from the [CurrentNetwork].
 */
fun interface NetworkAttributesExtractor : (AttributesBuilder, CurrentNetwork) -> Unit

/** Generates telemetry for when the network status changes.  */
@AutoService(AndroidInstrumentation::class)
class NetworkChangeInstrumentation : AndroidInstrumentation {
    private val additionalAttributeExtractors: MutableList<NetworkAttributesExtractor> = ArrayList()
    private var networkApplicationListener: NetworkApplicationListener? = null

    override val name = "network"

    /** Adds a [NetworkAttributesExtractor] that can add Attributes from the [CurrentNetwork].  */
    fun addAttributesExtractor(attributeExtractor: NetworkAttributesExtractor): NetworkChangeInstrumentation {
        additionalAttributeExtractors.add(attributeExtractor)
        return this
    }

    override fun install(context: Context, openTelemetryRum: OpenTelemetryRum) {
        additionalAttributeExtractors.add(NetworkChangeAttributesExtractor())
        val services = get(context)
        val listener = NetworkApplicationListener(services.currentNetworkProvider)
        val logger = openTelemetryRum.openTelemetry.logsBridge["io.opentelemetry.network"]
        listener.startMonitoring(logger, additionalAttributeExtractors)
        services.appLifecycle.registerListener(listener)
        networkApplicationListener = listener
    }

    override fun uninstall(context: Context, openTelemetryRum: OpenTelemetryRum) {
        networkApplicationListener?.let { listener ->
            listener.stopMonitoring()
            val services = get(context)
            services.appLifecycle.unregisterListener(listener)
        }
        networkApplicationListener = null
    }
}
