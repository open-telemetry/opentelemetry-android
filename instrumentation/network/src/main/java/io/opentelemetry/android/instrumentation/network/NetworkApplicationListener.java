/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork;
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener;
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider;
import io.opentelemetry.android.internal.services.network.NetworkChangeListener;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

class NetworkApplicationListener implements ApplicationStateListener {
    static final AttributeKey<String> NETWORK_STATUS_KEY = stringKey("network.status");

    private final CurrentNetworkProvider currentNetworkProvider;
    private final AtomicBoolean shouldEmitChangeEvents = new AtomicBoolean(true);

    NetworkApplicationListener(CurrentNetworkProvider currentNetworkProvider) {
        this.currentNetworkProvider = currentNetworkProvider;
    }

    void startMonitoring(
            Logger eventLogger,
            List<BiConsumer<AttributesBuilder, CurrentNetwork>> additionalExtractors) {
        currentNetworkProvider.addNetworkChangeListener(
                new TracingNetworkChangeListener(
                        eventLogger, shouldEmitChangeEvents, additionalExtractors));
    }

    @Override
    public void onApplicationForegrounded() {
        shouldEmitChangeEvents.set(true);
    }

    @Override
    public void onApplicationBackgrounded() {
        shouldEmitChangeEvents.set(false);
    }

    private static final class TracingNetworkChangeListener implements NetworkChangeListener {

        private final AtomicBoolean shouldEmitChangeEvents;
        private final Logger eventLogger;
        private final List<BiConsumer<AttributesBuilder, CurrentNetwork>> additionalExtractors;

        TracingNetworkChangeListener(
                Logger eventLogger,
                AtomicBoolean shouldEmitChangeEvents,
                List<BiConsumer<AttributesBuilder, CurrentNetwork>> additionalExtractors) {
            this.eventLogger = eventLogger;
            this.shouldEmitChangeEvents = shouldEmitChangeEvents;
            this.additionalExtractors = additionalExtractors;
        }

        @Override
        public void onNetworkChange(CurrentNetwork currentNetwork) {
            if (!shouldEmitChangeEvents.get()) {
                return;
            }
            AttributesBuilder attributesBuilder = Attributes.builder();
            additionalExtractors.forEach(
                    extractor -> extractor.accept(attributesBuilder, currentNetwork));

            ExtendedLogRecordBuilder builder =
                    (ExtendedLogRecordBuilder) eventLogger.logRecordBuilder();
            builder.setEventName("network.change")
                    .setAllAttributes(attributesBuilder.build())
                    .emit();
        }
    }
}
