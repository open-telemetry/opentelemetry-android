/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import androidx.core.content.ContextCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
public class CarrierFinderTest {

    private Context context;
    private TelephonyManager telephonyManager;
    private PackageManager packageManager;
    private CarrierFinder carrierFinder;
    private MockedStatic<ContextCompat> contextCompatMock;

    @Before
    public void setUp() {
        context = mock(Context.class);
        telephonyManager = mock(TelephonyManager.class);
        packageManager = mock(PackageManager.class);
        contextCompatMock = mockStatic(ContextCompat.class);

        when(context.getPackageManager()).thenReturn(packageManager);

        // Mock permission granted by default unless overridden in a test
        contextCompatMock
                .when(
                        () ->
                                ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            contextCompatMock
                    .when(
                            () ->
                                    ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.READ_BASIC_PHONE_STATE))
                    .thenReturn(PackageManager.PERMISSION_GRANTED);
        }

        carrierFinder = new CarrierFinder(context, telephonyManager);
    }

    @After
    public void tearDown() {
        if (contextCompatMock != null) {
            contextCompatMock.close();
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void testPostApi28WithPermission() {
        setupTelephonyCapability(true);

        when(telephonyManager.getSimCarrierId()).thenReturn(123);
        when(telephonyManager.getSimCarrierIdName()).thenReturn("TestCarrier");
        when(telephonyManager.getSimOperator()).thenReturn("31026");
        when(telephonyManager.getSimCountryIso()).thenReturn("nl");

        Carrier carrier = carrierFinder.get();

        assertThat(carrier.getId()).isEqualTo(123);
        assertThat(carrier.getName()).isEqualTo("TestCarrier");
        assertThat(carrier.getMobileCountryCode()).isEqualTo("310");
        assertThat(carrier.getMobileNetworkCode()).isEqualTo("26");
        assertThat(carrier.getIsoCountryCode()).isEqualTo("nl");
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void testPostApi28WithoutPermission() {
        setupTelephonyCapability(true);

        contextCompatMock
                .when(
                        () ->
                                ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        when(telephonyManager.getSimOperatorName()).thenReturn("testCarrier");
        when(telephonyManager.getSimOperator()).thenReturn("31026");
        when(telephonyManager.getSimCountryIso()).thenReturn("nl");

        Carrier carrier = carrierFinder.get();

        assertThat(carrier.getId()).isEqualTo(-1);
        assertThat(carrier.getName()).isEqualTo("testCarrier");
        assertThat(carrier.getMobileCountryCode()).isEqualTo("310");
        assertThat(carrier.getMobileNetworkCode()).isEqualTo("26");
        assertThat(carrier.getIsoCountryCode()).isEqualTo("nl");
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void testPreApi28WithValidName() {
        setupTelephonyCapability(true);

        when(telephonyManager.getSimOperatorName()).thenReturn("");
        when(telephonyManager.getNetworkOperatorName()).thenReturn("LegacyCarrier");
        when(telephonyManager.getSimOperator()).thenReturn("31026");
        when(telephonyManager.getSimCountryIso()).thenReturn("nl");

        Carrier carrier = carrierFinder.get();

        assertThat(carrier.getId()).isEqualTo(-1);
        assertThat(carrier.getName()).isEqualTo("LegacyCarrier");
        assertThat(carrier.getMobileCountryCode()).isEqualTo("310");
        assertThat(carrier.getMobileNetworkCode()).isEqualTo("26");
        assertThat(carrier.getIsoCountryCode()).isEqualTo("nl");
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void testPreApi28WithInvalidName() {
        setupTelephonyCapability(true);

        when(telephonyManager.getSimOperatorName()).thenReturn("");
        when(telephonyManager.getNetworkOperatorName()).thenReturn("");
        when(telephonyManager.getSimOperator()).thenReturn("31026");
        when(telephonyManager.getSimCountryIso()).thenReturn("nl");

        Carrier carrier = carrierFinder.get();

        assertThat(carrier.getId()).isEqualTo(-1);
        assertThat(carrier.getName()).isNull();
        assertThat(carrier.getMobileCountryCode()).isEqualTo("310");
        assertThat(carrier.getMobileNetworkCode()).isEqualTo("26");
        assertThat(carrier.getIsoCountryCode()).isEqualTo("nl");
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.TIRAMISU)
    public void testApi33WithBasicPhoneStatePermission() {
        setupTelephonyCapability(true);

        // Deny READ_PHONE_STATE but grant READ_BASIC_PHONE_STATE
        contextCompatMock
                .when(
                        () ->
                                ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        contextCompatMock
                .when(
                        () ->
                                ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.READ_BASIC_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        when(telephonyManager.getSimCarrierId()).thenReturn(456);
        when(telephonyManager.getSimCarrierIdName()).thenReturn("API33Carrier");
        when(telephonyManager.getSimOperator()).thenReturn("31026");
        when(telephonyManager.getSimCountryIso()).thenReturn("nl");

        Carrier carrier = carrierFinder.get();

        assertThat(carrier.getId()).isEqualTo(456);
        assertThat(carrier.getName()).isEqualTo("API33Carrier");
        assertThat(carrier.getMobileCountryCode()).isEqualTo("310");
        assertThat(carrier.getMobileNetworkCode()).isEqualTo("26");
        assertThat(carrier.getIsoCountryCode()).isEqualTo("nl");
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.TIRAMISU)
    public void testApi33WithoutAnyPermission() {
        setupTelephonyCapability(true);

        // Deny both permissions
        contextCompatMock
                .when(
                        () ->
                                ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        contextCompatMock
                .when(
                        () ->
                                ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.READ_BASIC_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        when(telephonyManager.getSimOperator()).thenReturn("31026");
        when(telephonyManager.getSimCountryIso()).thenReturn("nl");

        Carrier carrier = carrierFinder.get();

        assertThat(carrier.getId()).isEqualTo(-1);
        assertThat(carrier.getName()).isNull();
        assertThat(carrier.getMobileCountryCode()).isEqualTo("310");
        assertThat(carrier.getMobileNetworkCode()).isEqualTo("26");
        assertThat(carrier.getIsoCountryCode()).isEqualTo("nl");
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void testNoTelephonyCapability() {
        setupTelephonyCapability(false);

        Carrier carrier = carrierFinder.get();

        assertThat(carrier).isNull();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void testSecurityExceptionOnCarrierAccess() {
        setupTelephonyCapability(true);

        when(telephonyManager.getSimCarrierId())
                .thenThrow(new SecurityException("Permission denied"));

        Carrier carrier = carrierFinder.get();

        assertThat(carrier).isNull();
    }

    @Test
    public void testShortSimOperator() {
        setupTelephonyCapability(true);

        when(telephonyManager.getSimOperator()).thenReturn("123"); // Too short
        when(telephonyManager.getSimCountryIso()).thenReturn("nl");

        Carrier carrier = carrierFinder.get();

        assertThat(carrier.getMobileCountryCode()).isNull();
        assertThat(carrier.getMobileNetworkCode()).isNull();
        assertThat(carrier.getIsoCountryCode()).isEqualTo("nl");
    }

    @Test
    public void testInvalidSimOperator() {
        setupTelephonyCapability(true);

        when(telephonyManager.getSimOperator()).thenReturn("");
        when(telephonyManager.getSimCountryIso()).thenReturn("nl");

        Carrier carrier = carrierFinder.get();

        assertThat(carrier.getMobileCountryCode()).isNull();
        assertThat(carrier.getMobileNetworkCode()).isNull();
        assertThat(carrier.getIsoCountryCode()).isEqualTo("nl");
    }

    @Test
    public void testInvalidIsoCountryCode() {
        setupTelephonyCapability(true);

        when(telephonyManager.getSimOperator()).thenReturn("31026");
        when(telephonyManager.getSimCountryIso()).thenReturn("");

        Carrier carrier = carrierFinder.get();

        assertThat(carrier.getMobileCountryCode()).isEqualTo("310");
        assertThat(carrier.getMobileNetworkCode()).isEqualTo("26");
        assertThat(carrier.getIsoCountryCode()).isNull();
    }

    private void setupTelephonyCapability(boolean hasCapability) {
        when(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
                .thenReturn(hasCapability);
    }
}
