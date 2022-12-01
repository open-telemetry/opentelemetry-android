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

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import android.os.Build;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(sdk = Build.VERSION_CODES.P)
@RunWith(RobolectricTestRunner.class)
public class NetworkMonitorTest {

    @Rule public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();

    private Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void networkAvailable_wifi() {
        NetworkMonitor.TracingNetworkChangeListener listener =
                new NetworkMonitor.TracingNetworkChangeListener(tracer, new AtomicBoolean(true));

        listener.onNetworkChange(CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build());

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertThat(spans.get(0))
                .hasName("network.change")
                .hasAttributesSatisfyingExactly(
                        equalTo(NetworkMonitor.NETWORK_STATUS_KEY, "available"),
                        equalTo(SemanticAttributes.NET_HOST_CONNECTION_TYPE, "wifi"));
    }

    @Test
    public void networkAvailable_cellular() {
        NetworkMonitor.TracingNetworkChangeListener listener =
                new NetworkMonitor.TracingNetworkChangeListener(tracer, new AtomicBoolean(true));

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
                        equalTo(NetworkMonitor.NETWORK_STATUS_KEY, "available"),
                        equalTo(SemanticAttributes.NET_HOST_CONNECTION_TYPE, "cell"),
                        equalTo(SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE, "LTE"),
                        equalTo(SemanticAttributes.NET_HOST_CARRIER_NAME, "ShadyTel"),
                        equalTo(SemanticAttributes.NET_HOST_CARRIER_ICC, "US"),
                        equalTo(SemanticAttributes.NET_HOST_CARRIER_MCC, "usa"),
                        equalTo(SemanticAttributes.NET_HOST_CARRIER_MNC, "omg"));
    }

    @Test
    public void networkLost() {
        NetworkMonitor.TracingNetworkChangeListener listener =
                new NetworkMonitor.TracingNetworkChangeListener(tracer, new AtomicBoolean(true));

        listener.onNetworkChange(CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build());

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertThat(spans.get(0))
                .hasName("network.change")
                .hasAttributesSatisfyingExactly(
                        equalTo(NetworkMonitor.NETWORK_STATUS_KEY, "lost"),
                        equalTo(SemanticAttributes.NET_HOST_CONNECTION_TYPE, "unavailable"));
    }

    @Test
    public void noEventsPlease() {
        AtomicBoolean shouldEmitChangeEvents = new AtomicBoolean(false);

        NetworkMonitor.TracingNetworkChangeListener listener =
                new NetworkMonitor.TracingNetworkChangeListener(tracer, shouldEmitChangeEvents);

        listener.onNetworkChange(CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build());
        assertTrue(otelTesting.getSpans().isEmpty());
        listener.onNetworkChange(
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR).subType("LTE").build());
        assertTrue(otelTesting.getSpans().isEmpty());

        shouldEmitChangeEvents.set(true);
        listener.onNetworkChange(CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build());
        assertEquals(1, otelTesting.getSpans().size());
    }
}
