/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network

import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.NetworkChangeListener
import io.opentelemetry.android.ktx.setSessionIdentifiersWith
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

val NETWORK_STATUS_KEY: AttributeKey<String> = AttributeKey.stringKey("network.status")

@OptIn(Incubating::class)
internal class NetworkApplicationListener(
    private val currentNetworkProvider: CurrentNetworkProvider,
    private val sessionProvider: SessionProvider,
) : ApplicationStateListener {
    private val shouldEmitChangeEvents = AtomicBoolean(true)

    fun startMonitoring(
        eventLogger: Logger,
        additionalExtractors: List<NetworkAttributesExtractor>,
    ) {
        currentNetworkProvider.addNetworkChangeListener(
            TracingNetworkChangeListener(
                eventLogger,
                sessionProvider,
                shouldEmitChangeEvents,
                additionalExtractors,
            ),
        )
    }

    override fun onApplicationForegrounded() {
        shouldEmitChangeEvents.set(true)
    }

    override fun onApplicationBackgrounded() {
        shouldEmitChangeEvents.set(false)
    }

    private class TracingNetworkChangeListener(
        private val eventLogger: Logger,
        private val sessionProvider: SessionProvider,
        private val shouldEmitChangeEvents: AtomicBoolean,
        private val additionalExtractors: List<NetworkAttributesExtractor>,
    ) : NetworkChangeListener {
        override fun onNetworkChange(currentNetwork: CurrentNetwork) {
            if (!shouldEmitChangeEvents.get()) {
                return
            }
            val attributesBuilder = Attributes.builder()
            additionalExtractors.forEach(
                Consumer { extractor: NetworkAttributesExtractor ->
                    extractor(attributesBuilder, currentNetwork)
                },
            )
            val builder = eventLogger.logRecordBuilder()
            builder
                .setSessionIdentifiersWith(sessionProvider)
                .setEventName("network.change")
                .setAllAttributes(attributesBuilder.build())
                .emit()
        }
    }
}
