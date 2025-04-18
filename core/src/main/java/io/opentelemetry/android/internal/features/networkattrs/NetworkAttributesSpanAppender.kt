/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.networkattrs

import io.opentelemetry.android.common.internal.features.networkattributes.CurrentNetworkAttributesExtractor
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

/**
 * A [SpanProcessor] implementation that appends a set of [attributes][Attributes]
 * describing the [current network][CurrentNetwork] to every span that is exported.
 */
internal class NetworkAttributesSpanAppender(
    private val currentNetworkProvider: CurrentNetworkProvider,
) : SpanProcessor {
    private val networkAttributesExtractor = CurrentNetworkAttributesExtractor()

    override fun onStart(
        parentContext: Context,
        span: ReadWriteSpan,
    ) {
        val currentNetwork = currentNetworkProvider.currentNetwork
        span.setAllAttributes(networkAttributesExtractor.extract(currentNetwork))
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {}

    override fun isEndRequired(): Boolean = false

    companion object {
        @JvmStatic
        fun create(currentNetworkProvider: CurrentNetworkProvider): SpanProcessor = NetworkAttributesSpanAppender(currentNetworkProvider)
    }
}
