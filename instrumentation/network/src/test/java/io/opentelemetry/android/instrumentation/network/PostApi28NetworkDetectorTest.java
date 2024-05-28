/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.telephony.TelephonyManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class PostApi28NetworkDetectorTest {

    ConnectivityManager connectivityManager;
    TelephonyManager telephonyManager;
    Context context;
    Network network;
    NetworkCapabilities networkCapabilities;
    CarrierFinder carrierFinder;

    @Before
    public void setup() {
        connectivityManager = mock(ConnectivityManager.class);
        telephonyManager = mock(TelephonyManager.class);
        context = mock(Context.class);
        network = mock(Network.class);
        carrierFinder = Mockito.mock(CarrierFinder.class);
        networkCapabilities = mock(NetworkCapabilities.class);

        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
    }

    @Test
    public void none() {
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(null);

        PostApi28NetworkDetector networkDetector =
                new PostApi28NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build(), currentNetwork);
    }

    @Test
    public void wifi() {
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true);

        PostApi28NetworkDetector networkDetector =
                new PostApi28NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build(), currentNetwork);
    }

    @Test
    public void cellular() {
        when(telephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_LTE);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);

        PostApi28NetworkDetector networkDetector =
                new PostApi28NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR).subType("LTE").build(),
                currentNetwork);
    }

    @Test
    public void cellular_noTelephonyPermissions() {
        when(telephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_LTE);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);

        PostApi28NetworkDetector networkDetector =
                new PostApi28NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context) {
                    @Override
                    boolean canReadPhoneState() {
                        return false;
                    }
                };
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR).build(), currentNetwork);
    }

    @Test
    public void other() {
        PostApi28NetworkDetector networkDetector =
                new PostApi28NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_UNKNOWN).build(), currentNetwork);
    }

    @Test
    public void vpn() {
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)).thenReturn(true);

        PostApi28NetworkDetector networkDetector =
                new PostApi28NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_VPN).build(), currentNetwork);
    }

    @Test
    public void carrierIsSet() {
        Carrier carrier = Carrier.builder().name("flib").build();
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);
        when(carrierFinder.get()).thenReturn(carrier);
        PostApi28NetworkDetector networkDetector =
                new PostApi28NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertThat(currentNetwork.getCarrierName()).isEqualTo("flib");
    }
}
