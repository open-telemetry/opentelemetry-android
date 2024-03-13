/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.networ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.telephony.TelephonyManager;

import org.assertj.core.api.Assertions;
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
        Assertions.assertThat(carrier).isEqualTo(expected);
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
        Assertions.assertThat(carrier).isEqualTo(expected);
    }
}
