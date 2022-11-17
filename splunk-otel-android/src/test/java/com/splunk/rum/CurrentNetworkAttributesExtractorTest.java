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

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import android.os.Build;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
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

        assertThat(underTest.extract(currentNetwork))
                .containsOnly(
                        entry(SemanticAttributes.NET_HOST_CONNECTION_TYPE, "cell"),
                        entry(SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE, "aaa"),
                        entry(SemanticAttributes.NET_HOST_CARRIER_NAME, "ShadyTel"),
                        entry(SemanticAttributes.NET_HOST_CARRIER_ICC, "US"),
                        entry(SemanticAttributes.NET_HOST_CARRIER_MCC, "usa"),
                        entry(SemanticAttributes.NET_HOST_CARRIER_MNC, "omg"));
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @Test
    public void getNetworkAttributes_withoutCarrier() {
        CurrentNetwork currentNetwork =
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                        .subType("aaa")
                        .carrier(Carrier.builder().id(42).name("ShadyTel").build())
                        .build();

        assertThat(underTest.extract(currentNetwork))
                .containsOnly(
                        entry(SemanticAttributes.NET_HOST_CONNECTION_TYPE, "cell"),
                        entry(SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE, "aaa"));
    }
}
