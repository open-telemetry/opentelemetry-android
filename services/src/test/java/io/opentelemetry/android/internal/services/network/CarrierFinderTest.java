/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.telephony.TelephonyManager;
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier;
import org.junit.jupiter.api.Test;

class CarrierFinderTest {

    @Test
    void testSimpleGet() {
        Carrier expected = new Carrier(206, "ShadyTel", "usa", "omg", "US");

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
        Carrier expected = new Carrier();

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
