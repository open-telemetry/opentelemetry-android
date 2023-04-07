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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

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

        assertEquals(
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

        assertEquals(
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

        assertEquals(CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build(), currentNetwork);
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

        assertEquals(CurrentNetwork.builder(NetworkState.TRANSPORT_VPN).build(), currentNetwork);
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

        assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR).subType("LTE").build(),
                currentNetwork);
    }
}
