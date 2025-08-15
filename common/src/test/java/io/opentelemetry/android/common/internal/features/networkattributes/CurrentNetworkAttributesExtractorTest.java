/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.features.networkattributes;

import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_ICC;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MCC;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MNC;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_NAME;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE;
import static org.assertj.core.api.Assertions.entry;

import android.os.Build;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier;
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork;
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
public class CurrentNetworkAttributesExtractorTest {

    final CurrentNetworkAttributesExtractor underTest = new CurrentNetworkAttributesExtractor();

    @Config(sdk = Build.VERSION_CODES.P)
    @Test
    public void getNetworkAttributes_withCarrier() {
        CurrentNetwork currentNetwork =
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                        .subType("aaa")
                        .carrier(new Carrier(206, "ShadyTel", "usa", "omg", "US"))
                        .build();

        OpenTelemetryAssertions.assertThat(underTest.extract(currentNetwork))
                .containsOnly(
                        entry(NETWORK_CONNECTION_TYPE, "cell"),
                        entry(NETWORK_CONNECTION_SUBTYPE, "aaa"),
                        entry(NETWORK_CARRIER_NAME, "ShadyTel"),
                        entry(NETWORK_CARRIER_ICC, "US"),
                        entry(NETWORK_CARRIER_MCC, "usa"),
                        entry(NETWORK_CARRIER_MNC, "omg"));
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @Test
    public void getNetworkAttributes_withoutCarrier() {
        CurrentNetwork currentNetwork =
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR).subType("aaa").build();

        OpenTelemetryAssertions.assertThat(underTest.extract(currentNetwork))
                .containsOnly(
                        entry(NETWORK_CONNECTION_TYPE, "cell"),
                        entry(NETWORK_CONNECTION_SUBTYPE, "aaa"));
    }
}
