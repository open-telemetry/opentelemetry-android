/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.networkattrs;

import static org.assertj.core.api.Assertions.entry;

import android.os.Build;
import io.opentelemetry.android.internal.services.network.data.Carrier;
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;
import io.opentelemetry.android.internal.services.network.data.NetworkState;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.semconv.SemanticAttributes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class CurrentNetworkAttributesExtractorTest {

    final CurrentNetworkAttributesExtractor underTest = new CurrentNetworkAttributesExtractor();

    @Config(sdk = Build.VERSION_CODES.P)
    @Test
    public void getNetworkAttributes_withCarrier() {
        CurrentNetwork currentNetwork =
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                        .subType("aaa")
                        .carrier(
                                Carrier.builder()
                                        .id(206)
                                        .name("ShadyTel")
                                        .isoCountryCode("US")
                                        .mobileCountryCode("usa")
                                        .mobileNetworkCode("omg")
                                        .build())
                        .build();

        OpenTelemetryAssertions.assertThat(underTest.extract(currentNetwork))
                .containsOnly(
                        entry(SemanticAttributes.NETWORK_CONNECTION_TYPE, "cell"),
                        entry(SemanticAttributes.NETWORK_CONNECTION_SUBTYPE, "aaa"),
                        entry(SemanticAttributes.NETWORK_CARRIER_NAME, "ShadyTel"),
                        entry(SemanticAttributes.NETWORK_CARRIER_ICC, "US"),
                        entry(SemanticAttributes.NETWORK_CARRIER_MCC, "usa"),
                        entry(SemanticAttributes.NETWORK_CARRIER_MNC, "omg"));
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @Test
    public void getNetworkAttributes_withoutCarrier() {
        CurrentNetwork currentNetwork =
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                        .subType("aaa")
                        .carrier(Carrier.builder().id(42).name("ShadyTel").build())
                        .build();

        OpenTelemetryAssertions.assertThat(underTest.extract(currentNetwork))
                .containsOnly(
                        entry(SemanticAttributes.NETWORK_CONNECTION_TYPE, "cell"),
                        entry(SemanticAttributes.NETWORK_CONNECTION_SUBTYPE, "aaa"));
    }
}
