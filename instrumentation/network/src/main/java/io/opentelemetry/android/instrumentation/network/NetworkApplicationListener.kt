/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network

import android.util.Log
import androidx.annotation.VisibleForTesting
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener
import io.opentelemetry.android.internal.services.network.CurrentNetwork
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.NetworkChangeListener
import io.opentelemetry.android.semconv.events.NetworkChangeEvent
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import java.util.concurrent.atomic.AtomicBoolean

internal class NetworkApplicationListener(
    private val currentNetworkProvider: CurrentNetworkProvider,
) : ApplicationStateListener {
    private val shouldEmitChangeEvents = AtomicBoolean(true)
    private var networkChangeListener: TracingNetworkChangeListener? = null

    @VisibleForTesting
    internal val installedExtractors: List<NetworkAttributesExtractor>?
        get() = networkChangeListener?.additionalExtractors

    fun startMonitoring(
        eventLogger: Logger,
        additionalExtractors: List<NetworkAttributesExtractor>,
    ) {
        val listener =
            TracingNetworkChangeListener(
                eventLogger,
                shouldEmitChangeEvents,
                additionalExtractors,
            )
        networkChangeListener = listener
        currentNetworkProvider.addNetworkChangeListener(listener)
    }

    fun stopMonitoring() {
        networkChangeListener?.let {
            currentNetworkProvider.removeNetworkChangeListener(it)
        }
        networkChangeListener = null
    }

    override fun onApplicationForegrounded() {
        shouldEmitChangeEvents.set(true)
    }

    override fun onApplicationBackgrounded() {
        shouldEmitChangeEvents.set(false)
    }

    internal class TracingNetworkChangeListener(
        private val eventLogger: Logger,
        private val shouldEmitChangeEvents: AtomicBoolean,
        internal val additionalExtractors: List<NetworkAttributesExtractor>,
    ) : NetworkChangeListener {
        override fun onNetworkChange(currentNetwork: CurrentNetwork) {
            if (!shouldEmitChangeEvents.get()) {
                return
            }
            try {
                val attributesBuilder = Attributes.builder()
                additionalExtractors.forEach { extractor ->
                    try {
                        extractor(attributesBuilder, currentNetwork)
                    } catch (e: Exception) {
                        // a failing extractor (possibly user supplied) only drops its own attributes
                        Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Network attributes extractor failed", e)
                    }
                }
                NetworkChangeEvent().emit(eventLogger, attributesBuilder.build())
            } catch (e: Exception) {
                // this runs on the connectivity callback thread, so it must not throw
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to emit network change event", e)
            }
        }
    }
}
