/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier;
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork;
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNetworkInfo;

@RunWith(AndroidJUnit4.class)
public class NetworkDetectorTest {
    ConnectivityManager connectivityManager;
    TelephonyManager telephonyManager;
    Context context;
    Network network;
    NetworkCapabilities networkCapabilities;
    PackageManager packageManager;

    @Before
    public void setup() {
        connectivityManager = mock(ConnectivityManager.class);
        telephonyManager = mock(TelephonyManager.class);
        context = mock(Context.class);
        network = mock(Network.class);
        networkCapabilities = mock(NetworkCapabilities.class);
        packageManager = mock(PackageManager.class);

        when(context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(connectivityManager);
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
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

        // Setup for Carrier and SubType details as CarrierFinder and SubTypeFinder can't be
        // directly mocked here
        // as constructors can't be mocked with mockStatic
        when(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(true);
        when(context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PERMISSION_GRANTED);
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
        when(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(true);
        when(context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(telephonyManager.getSimCountryIso()).thenReturn("us");
        when(telephonyManager.getSimOperator()).thenReturn("310260");

        NetworkDetector networkDetector = NetworkDetector.create(context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        // Without permission, should still detect cellular but with limited info
        assertEquals(NetworkState.TRANSPORT_CELLULAR, currentNetwork.getState());
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
}
