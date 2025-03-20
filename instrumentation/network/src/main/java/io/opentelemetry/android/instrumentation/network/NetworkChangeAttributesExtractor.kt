/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE;

import io.opentelemetry.android.common.internal.features.networkattributes.CurrentNetworkAttributesExtractor;
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork;
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.function.BiConsumer;

final class NetworkChangeAttributesExtractor
        implements BiConsumer<AttributesBuilder, CurrentNetwork> {

    static final AttributeKey<String> NETWORK_STATUS_KEY = stringKey("network.status");

    private final CurrentNetworkAttributesExtractor networkAttributesExtractor =
            new CurrentNetworkAttributesExtractor();

    @Override
    public void accept(AttributesBuilder attributesBuilder, CurrentNetwork currentNetwork) {
        String status =
                currentNetwork.getState() == NetworkState.NO_NETWORK_AVAILABLE
                        ? "lost"
                        : "available";
        attributesBuilder.put(NETWORK_STATUS_KEY, status);
        if (currentNetwork.getState() == NetworkState.NO_NETWORK_AVAILABLE) {
            attributesBuilder.put(
                    NETWORK_CONNECTION_TYPE, currentNetwork.getState().getHumanName());
        } else {
            attributesBuilder.putAll(networkAttributesExtractor.extract(currentNetwork));
        }
    }
}
