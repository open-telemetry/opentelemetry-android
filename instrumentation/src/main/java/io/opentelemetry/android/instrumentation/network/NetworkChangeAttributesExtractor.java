/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.SemanticAttributes.NETWORK_CONNECTION_TYPE;

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
            attributes.put(NETWORK_CONNECTION_TYPE, currentNetwork.getState().getHumanName());
        } else {
            attributes.putAll(networkAttributesExtractor.extract(currentNetwork));
        }
    }
}
