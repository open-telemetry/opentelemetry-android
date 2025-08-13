/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNetworkInfo;

import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier;
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork;
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState;

@RunWith(AndroidJUnit4.class)
public class NetworkDetectorTest {
    ConnectivityManager connectivityManager;
    TelephonyManager telephonyManager;
    Context context;
    Network network;
    NetworkCapabilities networkCapabilities;
    PackageManager packageManager;
    MockedStatic<ContextCompat> contextCompatMock;

    @Before
    public void setup() {
        connectivityManager = mock(ConnectivityManager.class);
        telephonyManager = mock(TelephonyManager.class);
        context = mock(Context.class);
        network = mock(Network.class);
        networkCapabilities = mock(NetworkCapabilities.class);
        packageManager = mock(PackageManager.class);
        contextCompatMock = mockStatic(ContextCompat.class);

        when(context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(connectivityManager);
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
    }

    @After
    public void tearDown() {
        if (contextCompatMock != null) {
            contextCompatMock.close();
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void none_modern() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(null);
        NetworkDetector networkDetector = NetworkDetector.create(context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_UNKNOWN).build(), currentNetwork);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void wifi_modern() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true);
        NetworkDetector networkDetector = NetworkDetector.create(context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertEquals(CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build(), currentNetwork);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void cellular_modern() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);

        // Setup for Carrier and SubType details
        when(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(true);
        contextCompatMock.when(() -> ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(telephonyManager.getSimCarrierId()).thenReturn(310);
        when(telephonyManager.getSimCarrierIdName()).thenReturn("TestCarrier");
        when(telephonyManager.getSimCountryIso()).thenReturn("us");
        when(telephonyManager.getSimOperator()).thenReturn("310260");
        when(telephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_LTE);
        Carrier expectedCarrier = new Carrier(310, "TestCarrier", "310", "260", "us");

        NetworkDetector networkDetector = NetworkDetector.create(context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                        .carrier(expectedCarrier)
                        .subType("LTE")
                        .build(),
                currentNetwork);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void cellular_modern_withoutPermission() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);
        contextCompatMock.when(() -> ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        NetworkDetector networkDetector = NetworkDetector.create(context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        // Without permission, should still detect cellular but with no subtype
        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.getState());
        assertEquals(null, currentNetwork.getSubType());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void cellular_modern_noTelephonyFeature() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);
        when(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(false);
        NetworkDetector networkDetector = NetworkDetector.create(context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        // Without telephony feature, should still detect cellular but with limited info
        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.getState());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void other_modern() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        NetworkDetector networkDetector = NetworkDetector.create(context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_UNKNOWN).build(), currentNetwork);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void none_legacy() {
        ConnectivityManager cm =
                (ConnectivityManager)
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CONNECTIVITY_SERVICE);
        Shadows.shadowOf(cm).setActiveNetworkInfo(null);
        NetworkDetector networkDetector =
                NetworkDetector.create(ApplicationProvider.getApplicationContext());
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build(), currentNetwork);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void wifi_legacy() {
        ConnectivityManager cm =
                (ConnectivityManager)
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo =
                ShadowNetworkInfo.newInstance(
                        null, // Use null instead of deprecated DetailedState.CONNECTED
                        ConnectivityManager.TYPE_WIFI,
                        0,
                        true,
                        null); // Use null instead of deprecated State.CONNECTED
        Shadows.shadowOf(cm).setActiveNetworkInfo(networkInfo);
        NetworkDetector networkDetector =
                NetworkDetector.create(ApplicationProvider.getApplicationContext());
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build(), currentNetwork);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void cellularWithSubtype_legacy() {
        ConnectivityManager cm =
                (ConnectivityManager)
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(networkInfo.getSubtypeName()).thenReturn("LTE");
        Shadows.shadowOf(cm).setActiveNetworkInfo(networkInfo);
        NetworkDetector networkDetector =
                NetworkDetector.create(ApplicationProvider.getApplicationContext());
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        Assert.assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.getState());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    public void subtype_preApi24_withPermission() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);
        contextCompatMock.when(() -> ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_UMTS);

        NetworkDetector networkDetector = NetworkDetector.create(context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();

        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.getState());
        assertEquals("UMTS", currentNetwork.getSubType());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N)
    public void subtype_securityException_postApi24() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);
        contextCompatMock.when(() -> ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(telephonyManager.getDataNetworkType())
                .thenThrow(new SecurityException("Permission denied"));

        NetworkDetector networkDetector = NetworkDetector.create(context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();

        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.getState());
        assertEquals(null, currentNetwork.getSubType());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    public void subtype_securityException_preApi24() {
        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);
        contextCompatMock.when(() -> ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(telephonyManager.getNetworkType())
                .thenThrow(new SecurityException("Permission denied"));

        NetworkDetector networkDetector = NetworkDetector.create(context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();

        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.getState());
        assertEquals(null, currentNetwork.getSubType());
    }
}
