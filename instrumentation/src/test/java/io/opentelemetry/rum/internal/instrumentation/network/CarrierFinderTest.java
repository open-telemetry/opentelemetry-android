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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.telephony.TelephonyManager;
import org.junit.jupiter.api.Test;

class CarrierFinderTest {

    @Test
    void testSimpleGet() {
        Carrier expected =
                Carrier.builder()
                        .id(206)
                        .name("ShadyTel")
                        .isoCountryCode("US")
                        .mobileCountryCode("usa")
                        .mobileNetworkCode("omg")
                        .build();

        TelephonyManager manager = mock(TelephonyManager.class);
        when(manager.getSimCarrierId()).thenReturn(expected.getId());
        when(manager.getSimCarrierIdName()).thenReturn(expected.getName());
        when(manager.getSimCountryIso()).thenReturn(expected.getIsoCountryCode());
        when(manager.getSimOperator())
                .thenReturn(expected.getMobileCountryCode() + expected.getMobileNetworkCode());

        CarrierFinder finder = new CarrierFinder(manager);
        Carrier carrier = finder.get();
        assertThat(carrier).isEqualTo(expected);
    }

    @Test
    void testMostlyInvalid() {
        Carrier expected = Carrier.builder().build();

        TelephonyManager manager = mock(TelephonyManager.class);
        when(manager.getSimCarrierId()).thenReturn(-1);
        when(manager.getSimCarrierIdName()).thenReturn(null);
        when(manager.getSimCountryIso()).thenReturn(null);
        when(manager.getSimOperator()).thenReturn(null);

        CarrierFinder finder = new CarrierFinder(manager);
        Carrier carrier = finder.get();
        assertThat(carrier).isEqualTo(expected);
    }
}
