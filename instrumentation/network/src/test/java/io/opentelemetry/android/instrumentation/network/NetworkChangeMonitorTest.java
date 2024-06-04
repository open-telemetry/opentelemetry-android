/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import android.os.Build;
import io.opentelemetry.android.instrumentation.common.ApplicationStateListener;
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider;
import io.opentelemetry.android.internal.services.network.NetworkChangeListener;
import io.opentelemetry.android.internal.services.network.data.Carrier;
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;
import io.opentelemetry.android.internal.services.network.data.NetworkState;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.SemanticAttributes;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(sdk = Build.VERSION_CODES.P)
@RunWith(RobolectricTestRunner.class)
public class NetworkChangeMonitorTest {

    @Rule public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();

    @Captor ArgumentCaptor<ApplicationStateListener> applicationStateListenerCaptor;
    @Captor ArgumentCaptor<NetworkChangeListener> networkChangeListenerCaptor;

    @Mock CurrentNetworkProvider currentNetworkProvider;

    AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void networkAvailable_wifi() {
        create().start();

        verify(currentNetworkProvider)
                .addNetworkChangeListener(networkChangeListenerCaptor.capture());
        NetworkChangeListener listener = networkChangeListenerCaptor.getValue();

        listener.onNetworkChange(CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build());

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertThat(spans.get(0))
                .hasName("network.change")
                .hasAttributesSatisfyingExactly(
                        equalTo(NetworkApplicationListener.NETWORK_STATUS_KEY, "available"),
                        equalTo(SemanticAttributes.NETWORK_CONNECTION_TYPE, "wifi"));
    }

    @Test
    public void networkAvailable_cellular() {
        create().start();

        verify(currentNetworkProvider)
                .addNetworkChangeListener(networkChangeListenerCaptor.capture());
        NetworkChangeListener listener = networkChangeListenerCaptor.getValue();

        CurrentNetwork network =
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                        .subType("LTE")
                        .carrier(
                                Carrier.builder()
                                        .id(206)
                                        .name("ShadyTel")
                                        .isoCountryCode("US")
                                        .mobileCountryCode("usa")
                                        .mobileNetworkCode("omg")
                                        .build())
                        .build();

        listener.onNetworkChange(network);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertThat(spans.get(0))
                .hasName("network.change")
                .hasAttributesSatisfyingExactly(
                        equalTo(NetworkApplicationListener.NETWORK_STATUS_KEY, "available"),
                        equalTo(SemanticAttributes.NETWORK_CONNECTION_TYPE, "cell"),
                        equalTo(SemanticAttributes.NETWORK_CONNECTION_SUBTYPE, "LTE"),
                        equalTo(SemanticAttributes.NETWORK_CARRIER_NAME, "ShadyTel"),
                        equalTo(SemanticAttributes.NETWORK_CARRIER_ICC, "US"),
                        equalTo(SemanticAttributes.NETWORK_CARRIER_MCC, "usa"),
                        equalTo(SemanticAttributes.NETWORK_CARRIER_MNC, "omg"));
    }

    @Test
    public void networkLost() {
        create().start();

        verify(currentNetworkProvider)
                .addNetworkChangeListener(networkChangeListenerCaptor.capture());
        NetworkChangeListener listener = networkChangeListenerCaptor.getValue();

        listener.onNetworkChange(CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build());

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertThat(spans.get(0))
                .hasName("network.change")
                .hasAttributesSatisfyingExactly(
                        equalTo(NetworkApplicationListener.NETWORK_STATUS_KEY, "lost"),
                        equalTo(SemanticAttributes.NETWORK_CONNECTION_TYPE, "unavailable"));
    }

    @Test
    @Ignore("Reintroduce in part 3")
    public void noEventsPlease() {
        create().start();

        verify(currentNetworkProvider)
                .addNetworkChangeListener(networkChangeListenerCaptor.capture());
        NetworkChangeListener networkListener = networkChangeListenerCaptor.getValue();

        //        verify(instrumentedApplication)
        //
        // .registerApplicationStateListener(applicationStateListenerCaptor.capture()); TODO
        // uncomment in part 3
        ApplicationStateListener applicationListener = applicationStateListenerCaptor.getValue();

        applicationListener.onApplicationBackgrounded();

        networkListener.onNetworkChange(
                CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build());
        assertTrue(otelTesting.getSpans().isEmpty());
        networkListener.onNetworkChange(
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR).subType("LTE").build());
        assertTrue(otelTesting.getSpans().isEmpty());

        applicationListener.onApplicationForegrounded();

        networkListener.onNetworkChange(
                CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build());
        assertEquals(1, otelTesting.getSpans().size());
    }

    private NetworkChangeMonitor create() {
        return new NetworkChangeMonitor(
                otelTesting.getOpenTelemetry(), currentNetworkProvider, Collections.emptyList());
    }
}
