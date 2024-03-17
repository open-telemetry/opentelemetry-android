/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(maxSdk = Build.VERSION_CODES.S)
public class CurrentNetworkProviderTest {

    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP)
    public void lollipop() {
        NetworkRequest networkRequest = mock(NetworkRequest.class);
        NetworkDetector networkDetector = Mockito.mock(NetworkDetector.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);

        when(networkDetector.detectCurrentNetwork())
                .thenReturn(
                        CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI)
                                .build()) // called on init
                .thenReturn(
                        CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                                .subType("LTE")
                                .build());

        CurrentNetworkProvider currentNetworkProvider = new CurrentNetworkProvider(networkDetector);
        currentNetworkProvider.startMonitoring(() -> networkRequest, connectivityManager);

        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build(),
                currentNetworkProvider.getCurrentNetwork());

        ArgumentCaptor<NetworkCallback> monitorCaptor =
                ArgumentCaptor.forClass(NetworkCallback.class);
        verify(connectivityManager)
                .registerNetworkCallback(eq(networkRequest), monitorCaptor.capture());

        AtomicInteger notified = new AtomicInteger(0);
        currentNetworkProvider.addNetworkChangeListener(
                (currentNetwork) -> {
                    int timesCalled = notified.incrementAndGet();
                    if (timesCalled == 1) {
                        Assert.assertEquals(
                                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                                        .subType("LTE")
                                        .build(),
                                currentNetwork);
                    } else {
                        Assert.assertEquals(
                                CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build(),
                                currentNetwork);
                    }
                });
        // note: we ignore the network passed in and just rely on refreshing the network info when
        // this is happens
        monitorCaptor.getValue().onAvailable(null);
        assertEquals(1, notified.get());
        monitorCaptor.getValue().onLost(null);
        assertEquals(2, notified.get());
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.S, minSdk = Build.VERSION_CODES.O)
    public void modernSdks() {
        NetworkRequest networkRequest = mock(NetworkRequest.class);
        NetworkDetector networkDetector = Mockito.mock(NetworkDetector.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);

        when(networkDetector.detectCurrentNetwork())
                .thenReturn(CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build())
                .thenReturn(
                        CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                                .subType("LTE")
                                .build());

        CurrentNetworkProvider currentNetworkProvider = new CurrentNetworkProvider(networkDetector);
        currentNetworkProvider.startMonitoring(() -> networkRequest, connectivityManager);

        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build(),
                currentNetworkProvider.getCurrentNetwork());
        verify(connectivityManager, never())
                .registerNetworkCallback(eq(networkRequest), isA(NetworkCallback.class));

        ArgumentCaptor<NetworkCallback> monitorCaptor =
                ArgumentCaptor.forClass(NetworkCallback.class);
        verify(connectivityManager).registerDefaultNetworkCallback(monitorCaptor.capture());

        AtomicInteger notified = new AtomicInteger(0);
        currentNetworkProvider.addNetworkChangeListener(
                (currentNetwork) -> {
                    int timesCalled = notified.incrementAndGet();
                    if (timesCalled == 1) {
                        Assert.assertEquals(
                                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                                        .subType("LTE")
                                        .build(),
                                currentNetwork);
                    } else {
                        Assert.assertEquals(
                                CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build(),
                                currentNetwork);
                    }
                });
        // note: we ignore the network passed in and just rely on refreshing the network info when
        // this is happens
        monitorCaptor.getValue().onAvailable(null);
        assertEquals(1, notified.get());
        monitorCaptor.getValue().onLost(null);
        assertEquals(2, notified.get());
    }

    @Test
    public void networkDetectorException() {
        NetworkDetector networkDetector = Mockito.mock(NetworkDetector.class);
        when(networkDetector.detectCurrentNetwork()).thenThrow(new SecurityException("bug"));

        CurrentNetworkProvider currentNetworkProvider = new CurrentNetworkProvider(networkDetector);
        Assert.assertEquals(
                CurrentNetworkProvider.UNKNOWN_NETWORK,
                currentNetworkProvider.refreshNetworkStatus());
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.S, minSdk = Build.VERSION_CODES.O)
    public void networkDetectorExceptionOnCallbackRegistration() {
        NetworkDetector networkDetector = Mockito.mock(NetworkDetector.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);

        when(networkDetector.detectCurrentNetwork())
                .thenReturn(CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build());
        doThrow(new SecurityException("bug"))
                .when(connectivityManager)
                .registerDefaultNetworkCallback(isA(NetworkCallback.class));

        CurrentNetworkProvider currentNetworkProvider = new CurrentNetworkProvider(networkDetector);
        currentNetworkProvider.startMonitoring(
                () -> mock(NetworkRequest.class), connectivityManager);
        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build(),
                currentNetworkProvider.refreshNetworkStatus());
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP)
    public void networkDetectorExceptionOnCallbackRegistration_lollipop() {
        NetworkDetector networkDetector = Mockito.mock(NetworkDetector.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        NetworkRequest networkRequest = mock(NetworkRequest.class);

        when(networkDetector.detectCurrentNetwork())
                .thenReturn(CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build());
        doThrow(new SecurityException("bug"))
                .when(connectivityManager)
                .registerNetworkCallback(eq(networkRequest), isA(NetworkCallback.class));

        CurrentNetworkProvider currentNetworkProvider = new CurrentNetworkProvider(networkDetector);
        currentNetworkProvider.startMonitoring(() -> networkRequest, connectivityManager);
        Assert.assertEquals(
                CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build(),
                currentNetworkProvider.refreshNetworkStatus());
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP)
    public void shouldNotFailOnImmediateConnectionManagerCall_lollipop() {
        NetworkRequest networkRequest = mock(NetworkRequest.class);
        NetworkDetector networkDetector = Mockito.mock(NetworkDetector.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);

        doAnswer(
                        invocation -> {
                            NetworkCallback callback = invocation.getArgument(1);
                            callback.onAvailable(mock(Network.class));
                            return null;
                        })
                .when(connectivityManager)
                .registerNetworkCallback(eq(networkRequest), any(NetworkCallback.class));

        CurrentNetworkProvider currentNetworkProvider = new CurrentNetworkProvider(networkDetector);
        currentNetworkProvider.startMonitoring(() -> networkRequest, connectivityManager);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.S, minSdk = Build.VERSION_CODES.O)
    public void shouldNotFailOnImmediateConnectionManagerCall() {
        NetworkRequest networkRequest = mock(NetworkRequest.class);
        NetworkDetector networkDetector = Mockito.mock(NetworkDetector.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);

        doAnswer(
                        invocation -> {
                            NetworkCallback callback = invocation.getArgument(0);
                            callback.onAvailable(mock(Network.class));
                            return null;
                        })
                .when(connectivityManager)
                .registerDefaultNetworkCallback(any(NetworkCallback.class));

        CurrentNetworkProvider currentNetworkProvider = new CurrentNetworkProvider(networkDetector);
        currentNetworkProvider.startMonitoring(() -> networkRequest, connectivityManager);
    }
}
