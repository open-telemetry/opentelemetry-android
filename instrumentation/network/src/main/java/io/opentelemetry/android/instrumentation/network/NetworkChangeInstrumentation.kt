/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network

import com.google.auto.service.AutoService
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
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

    override val name = "network"

    /** Adds a [NetworkAttributesExtractor] that can add Attributes from the [CurrentNetwork].  */
    fun addAttributesExtractor(attributeExtractor: NetworkAttributesExtractor): NetworkChangeInstrumentation {
        additionalAttributeExtractors.add(attributeExtractor)
        return this
    }

    override fun install(ctx: InstallationContext) {
        additionalAttributeExtractors.add(NetworkChangeAttributesExtractor())
        val services = get(ctx.context)
        val networkApplicationListener = NetworkApplicationListener(services.currentNetworkProvider, ctx.sessionProvider)
        val logger = ctx.openTelemetry.logsBridge["io.opentelemetry.network"]
        networkApplicationListener.startMonitoring(logger, additionalAttributeExtractors)
        services.appLifecycle.registerListener(networkApplicationListener)
    }
}
