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

import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ConnectionUtilTest {

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void lollipop() {
        NetworkRequest networkRequest = mock(NetworkRequest.class);
        NetworkDetector networkDetector = mock(NetworkDetector.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);

        when(networkDetector.detectCurrentNetwork())
                .thenReturn(new CurrentNetwork(NetworkState.TRANSPORT_WIFI, null)) //called on init
                .thenReturn(new CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, "LTE"));

        ConnectionUtil connectionUtil = new ConnectionUtil(() -> networkRequest, networkDetector, connectivityManager);

        assertTrue(connectionUtil.isOnline());
        assertEquals(new CurrentNetwork(NetworkState.TRANSPORT_WIFI, null), connectionUtil.getActiveNetwork());

        ArgumentCaptor<ConnectionUtil.ConnectionMonitor> monitorCaptor = ArgumentCaptor.forClass(ConnectionUtil.ConnectionMonitor.class);
        verify(connectivityManager).registerNetworkCallback(eq(networkRequest), monitorCaptor.capture());

        AtomicInteger notified = new AtomicInteger(0);
        connectionUtil.setInternetStateListener((deviceIsOnline, currentNetwork) -> {
            int timesCalled = notified.incrementAndGet();
            if (timesCalled == 1) {
                assertTrue(deviceIsOnline);
                assertEquals(new CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, "LTE"), currentNetwork);
            } else {
                assertFalse(deviceIsOnline);
                assertEquals(new CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE, null), currentNetwork);
            }
        });
        //note: we ignore the network passed in and just rely on refreshing the network info when this is happens
        monitorCaptor.getValue().onAvailable(null);
        assertEquals(1, notified.get());
        monitorCaptor.getValue().onLost(null);
        assertEquals(2, notified.get());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void quiznos() {
        NetworkRequest networkRequest = mock(NetworkRequest.class);
        NetworkDetector networkDetector = mock(NetworkDetector.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);

        when(networkDetector.detectCurrentNetwork())
                .thenReturn(new CurrentNetwork(NetworkState.TRANSPORT_WIFI, null))
                .thenReturn(new CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, "LTE"));

        ConnectionUtil connectionUtil = new ConnectionUtil(() -> networkRequest, networkDetector, connectivityManager);

        assertTrue(connectionUtil.isOnline());
        assertEquals(new CurrentNetwork(NetworkState.TRANSPORT_WIFI, null), connectionUtil.getActiveNetwork());
        verify(connectivityManager, never()).registerNetworkCallback(eq(networkRequest), isA(ConnectionUtil.ConnectionMonitor.class));

        ArgumentCaptor<ConnectionUtil.ConnectionMonitor> monitorCaptor = ArgumentCaptor.forClass(ConnectionUtil.ConnectionMonitor.class);
        verify(connectivityManager).registerDefaultNetworkCallback(monitorCaptor.capture());

        AtomicInteger notified = new AtomicInteger(0);
        connectionUtil.setInternetStateListener((deviceIsOnline, currentNetwork) -> {
            int timesCalled = notified.incrementAndGet();
            if (timesCalled == 1) {
                assertTrue(deviceIsOnline);
                assertEquals(new CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, "LTE"), currentNetwork);
            } else {
                assertFalse(deviceIsOnline);
                assertEquals(new CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE, null), currentNetwork);
            }
        });
        //note: we ignore the network passed in and just rely on refreshing the network info when this is happens
        monitorCaptor.getValue().onAvailable(null);
        assertEquals(1, notified.get());
        monitorCaptor.getValue().onLost(null);
        assertEquals(2, notified.get());
    }
}