/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.android.instrumentation.common.ApplicationStateListener;
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider;
import io.opentelemetry.android.internal.services.network.NetworkChangeListener;
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.concurrent.atomic.AtomicBoolean;

class NetworkApplicationListener implements ApplicationStateListener {
    static final AttributeKey<String> NETWORK_STATUS_KEY = stringKey("network.status");

    private final CurrentNetworkProvider currentNetworkProvider;
    private final AtomicBoolean shouldEmitChangeEvents = new AtomicBoolean(true);

    NetworkApplicationListener(CurrentNetworkProvider currentNetworkProvider) {
        this.currentNetworkProvider = currentNetworkProvider;
    }

    void startMonitoring(Instrumenter<CurrentNetwork, Void> instrumenter) {
        currentNetworkProvider.addNetworkChangeListener(
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
