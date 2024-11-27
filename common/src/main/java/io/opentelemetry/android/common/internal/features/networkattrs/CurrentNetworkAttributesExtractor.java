/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.features.networkattrs;

import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_ICC;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MCC;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MNC;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_NAME;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE;

import androidx.annotation.Nullable;
import io.opentelemetry.android.common.internal.features.networkattrs.data.CurrentNetwork;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

public final class CurrentNetworkAttributesExtractor {

    public Attributes extract(CurrentNetwork network) {
        AttributesBuilder builder =
                Attributes.builder()
                        .put(NETWORK_CONNECTION_TYPE, network.getState().getHumanName());

        setIfNotNull(builder, NETWORK_CONNECTION_SUBTYPE, network.getSubType());
        setIfNotNull(builder, NETWORK_CARRIER_NAME, network.getCarrierName());
        setIfNotNull(builder, NETWORK_CARRIER_MCC, network.getCarrierCountryCode());
        setIfNotNull(builder, NETWORK_CARRIER_MNC, network.getCarrierNetworkCode());
        setIfNotNull(builder, NETWORK_CARRIER_ICC, network.getCarrierIsoCountryCode());

        return builder.build();
    }

    private static void setIfNotNull(
            AttributesBuilder builder, AttributeKey<String> key, @Nullable String value) {
        if (value != null) {
            builder.put(key, value);
        }
    }
}
