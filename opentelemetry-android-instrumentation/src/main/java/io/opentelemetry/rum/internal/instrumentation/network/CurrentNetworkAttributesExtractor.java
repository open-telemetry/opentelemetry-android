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

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_ICC;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_MCC;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_MNC;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_TYPE;

import androidx.annotation.Nullable;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

final class CurrentNetworkAttributesExtractor {

    Attributes extract(CurrentNetwork network) {
        AttributesBuilder builder =
                Attributes.builder()
                        .put(NET_HOST_CONNECTION_TYPE, network.getState().getHumanName());

        setIfNotNull(builder, NET_HOST_CONNECTION_SUBTYPE, network.getSubType());
        setIfNotNull(builder, NET_HOST_CARRIER_NAME, network.getCarrierName());
        setIfNotNull(builder, NET_HOST_CARRIER_MCC, network.getCarrierCountryCode());
        setIfNotNull(builder, NET_HOST_CARRIER_MNC, network.getCarrierNetworkCode());
        setIfNotNull(builder, NET_HOST_CARRIER_ICC, network.getCarrierIsoCountryCode());

        return builder.build();
    }

    private static void setIfNotNull(
            AttributesBuilder builder, AttributeKey<String> key, @Nullable String value) {
        if (value != null) {
            builder.put(key, value);
        }
    }
}
