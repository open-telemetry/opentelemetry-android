/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.rum.internal.instrumentation.network;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.rum.internal.instrumentation.ApplicationStateListener;

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
