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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

import static com.splunk.rum.RumAttributeAppender.NETWORK_SUBTYPE_KEY;
import static com.splunk.rum.RumAttributeAppender.NETWORK_TYPE_KEY;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

class NetworkMonitor {
    static final AttributeKey<String> NETWORK_STATUS_KEY = stringKey("network.status");

    private final ConnectionUtil connectionUtil;

    NetworkMonitor(ConnectionUtil connectionUtil) {
        this.connectionUtil = connectionUtil;
    }

    void addConnectivityListener(Tracer tracer) {
        connectionUtil.setInternetStateListener(new TracingConnectionStateListener(tracer));
    }

    //visibleForTesting
    static class TracingConnectionStateListener implements ConnectionStateListener {
        private final Tracer tracer;

        TracingConnectionStateListener(Tracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public void onAvailable(boolean deviceIsOnline, CurrentNetwork activeNetwork) {
            if (activeNetwork.getState() == NetworkState.NO_NETWORK_AVAILABLE) {
                tracer.spanBuilder("network.change")
                        .setAttribute(NETWORK_STATUS_KEY, "lost")
                        .startSpan()
                        //put this after span start to override what might be set in the RumAttributeAppender.
                        .setAttribute(NETWORK_TYPE_KEY, activeNetwork.getState().getHumanName())
                        .end();
            } else {
                Span available = tracer.spanBuilder("network.change")
                        .setAttribute(NETWORK_STATUS_KEY, "available")
                        .startSpan()
                        //put these after span start to override what might be set in the RumAttributeAppender.
                        .setAttribute(NETWORK_TYPE_KEY, activeNetwork.getState().getHumanName());
                activeNetwork.getSubType().ifPresent(subType -> available.setAttribute(NETWORK_SUBTYPE_KEY, subType));
                available.end();
            }
        }
    }
}
