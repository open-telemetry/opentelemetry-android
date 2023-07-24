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
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_TYPE;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

final class NetworkChangeAttributesExtractor implements AttributesExtractor<CurrentNetwork, Void> {

    static final AttributeKey<String> NETWORK_STATUS_KEY = stringKey("network.status");

    private final CurrentNetworkAttributesExtractor networkAttributesExtractor =
            new CurrentNetworkAttributesExtractor();

    @Override
    public void onStart(
            AttributesBuilder attributes, Context parentContext, CurrentNetwork currentNetwork) {
        String status =
                currentNetwork.getState() == NetworkState.NO_NETWORK_AVAILABLE
                        ? "lost"
                        : "available";
        attributes.put(NETWORK_STATUS_KEY, status);
    }

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            Context context,
            CurrentNetwork currentNetwork,
            Void unused,
            Throwable error) {
        // put these after span start to override what might be set in the
        // NetworkAttributesSpanAppender.
        if (currentNetwork.getState() == NetworkState.NO_NETWORK_AVAILABLE) {
            attributes.put(NET_HOST_CONNECTION_TYPE, currentNetwork.getState().getHumanName());
        } else {
            attributes.putAll(networkAttributesExtractor.extract(currentNetwork));
        }
    }
}
