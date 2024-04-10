/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener;
import io.opentelemetry.android.internal.services.network.CurrentNetworkService;
import io.opentelemetry.android.internal.services.network.NetworkChangeListener;
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.concurrent.atomic.AtomicBoolean;

class NetworkApplicationListener implements ApplicationStateListener {
    static final AttributeKey<String> NETWORK_STATUS_KEY = stringKey("network.status");

    private final CurrentNetworkService currentNetworkService;
    private final AtomicBoolean shouldEmitChangeEvents = new AtomicBoolean(true);

    NetworkApplicationListener(CurrentNetworkService currentNetworkService) {
        this.currentNetworkService = currentNetworkService;
    }

    void startMonitoring(Instrumenter<CurrentNetwork, Void> instrumenter) {
        currentNetworkService.addNetworkChangeListener(
                new TracingNetworkChangeListener(instrumenter, shouldEmitChangeEvents));
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

        private final Instrumenter<CurrentNetwork, Void> instrumenter;
        private final AtomicBoolean shouldEmitChangeEvents;

        TracingNetworkChangeListener(
                Instrumenter<CurrentNetwork, Void> instrumenter,
                AtomicBoolean shouldEmitChangeEvents) {
            this.instrumenter = instrumenter;
            this.shouldEmitChangeEvents = shouldEmitChangeEvents;
        }

        @Override
        public void onNetworkChange(CurrentNetwork currentNetwork) {
            if (!shouldEmitChangeEvents.get()) {
                return;
            }
            Context context = instrumenter.start(Context.current(), currentNetwork);
            instrumenter.end(context, currentNetwork, null, null);
        }
    }
}
