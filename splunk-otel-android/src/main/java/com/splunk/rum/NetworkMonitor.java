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

package com.splunk.rum;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_TYPE;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

class NetworkMonitor implements AppStateListener {
    static final AttributeKey<String> NETWORK_STATUS_KEY = stringKey("network.status");

    private final ConnectionUtil connectionUtil;
    private final AtomicBoolean shouldEmitChangeEvents = new AtomicBoolean(true);

    NetworkMonitor(ConnectionUtil connectionUtil) {
        this.connectionUtil = connectionUtil;
    }

    void addConnectivityListener(Tracer tracer) {
        connectionUtil.setInternetStateListener(
                new TracingConnectionStateListener(tracer, shouldEmitChangeEvents));
    }

    @Override
    public void appForegrounded() {
        shouldEmitChangeEvents.set(true);
    }

    @Override
    public void appBackgrounded() {
        shouldEmitChangeEvents.set(false);
    }

    // visibleForTesting
    static class TracingConnectionStateListener implements ConnectionStateListener {
        private final Tracer tracer;
        private final AtomicBoolean shouldEmitChangeEvents;

        TracingConnectionStateListener(Tracer tracer, AtomicBoolean shouldEmitChangeEvents) {
            this.tracer = tracer;
            this.shouldEmitChangeEvents = shouldEmitChangeEvents;
        }

        @Override
        public void onAvailable(boolean deviceIsOnline, CurrentNetwork activeNetwork) {
            if (!shouldEmitChangeEvents.get()) {
                return;
            }
            if (activeNetwork.getState() == NetworkState.NO_NETWORK_AVAILABLE) {
                tracer.spanBuilder("network.change")
                        .setAttribute(NETWORK_STATUS_KEY, "lost")
                        .startSpan()
                        // put this after span start to override what might be set in the
                        // RumAttributeAppender.
                        .setAttribute(
                                NET_HOST_CONNECTION_TYPE, activeNetwork.getState().getHumanName())
                        .end();
            } else {
                Span available =
                        tracer.spanBuilder("network.change")
                                .setAttribute(NETWORK_STATUS_KEY, "available")
                                .startSpan()
                                // put these after span start to override what might be set in the
                                // RumAttributeAppender.
                                .setAttribute(
                                        NET_HOST_CONNECTION_TYPE,
                                        activeNetwork.getState().getHumanName());
                activeNetwork
                        .getSubType()
                        .ifPresent(
                                subType ->
                                        available.setAttribute(
                                                SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE,
                                                subType));
                available.end();
            }
        }
    }
}
