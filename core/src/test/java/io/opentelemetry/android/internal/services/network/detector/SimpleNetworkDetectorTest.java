/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;
import io.opentelemetry.android.internal.services.network.data.NetworkState;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNetworkInfo;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class SimpleNetworkDetectorTest {
    @Test
    public void none() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CONNECTIVITY_SERVICE);

        Shadows.shadowOf(connectivityManager).setActiveNetworkInfo(null);
        SimpleNetworkDetector networkDetector = new SimpleNetworkDetector(connectivityManager);

        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();

        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build(), currentNetwork);
    }

    @Test
    public void other() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo =
                ShadowNetworkInfo.newInstance(
                        NetworkInfo.DetailedState.CONNECTED,
                        ConnectivityManager.TYPE_DUMMY,
                        0,
                        true,
                        NetworkInfo.State.CONNECTED);
        Shadows.shadowOf(connectivityManager).setActiveNetworkInfo(networkInfo);

        SimpleNetworkDetector networkDetector = new SimpleNetworkDetector(connectivityManager);

        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();

        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_UNKNOWN).build(), currentNetwork);
    }

    @Test
    public void wifi() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo =
                ShadowNetworkInfo.newInstance(
                        NetworkInfo.DetailedState.CONNECTED,
                        ConnectivityManager.TYPE_WIFI,
                        0,
                        true,
                        NetworkInfo.State.CONNECTED);
        Shadows.shadowOf(connectivityManager).setActiveNetworkInfo(networkInfo);

        SimpleNetworkDetector networkDetector = new SimpleNetworkDetector(connectivityManager);

        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();

        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build(), currentNetwork);
    }

    @Test
    public void vpn() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo =
                ShadowNetworkInfo.newInstance(
                        NetworkInfo.DetailedState.CONNECTED,
                        ConnectivityManager.TYPE_VPN,
                        0,
                        true,
                        NetworkInfo.State.CONNECTED);
        Shadows.shadowOf(connectivityManager).setActiveNetworkInfo(networkInfo);

        SimpleNetworkDetector networkDetector = new SimpleNetworkDetector(connectivityManager);

        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();

        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_VPN).build(), currentNetwork);
    }

    @Test
    public void wired() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo =
                ShadowNetworkInfo.newInstance(
                        NetworkInfo.DetailedState.CONNECTED,
                        ConnectivityManager.TYPE_ETHERNET,
                        0,
                        true,
                        NetworkInfo.State.CONNECTED);
        Shadows.shadowOf(connectivityManager).setActiveNetworkInfo(networkInfo);

        SimpleNetworkDetector networkDetector = new SimpleNetworkDetector(connectivityManager);

        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();

        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_WIRED).build(), currentNetwork);
    }

    @Test
    public void cellularWithSubtype() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CONNECTIVITY_SERVICE);

        // note: looks like the ShadowNetworkInfo doesn't support setting subtype name.
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(networkInfo.getSubtypeName()).thenReturn("LTE");

        Shadows.shadowOf(connectivityManager).setActiveNetworkInfo(networkInfo);

        SimpleNetworkDetector networkDetector = new SimpleNetworkDetector(connectivityManager);

        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();

        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR).subType("LTE").build(),
                currentNetwork);
    }
}
