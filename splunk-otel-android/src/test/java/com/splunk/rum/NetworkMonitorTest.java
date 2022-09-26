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

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class NetworkMonitorTest {
    @Rule public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    public void networkAvailable_wifi() {
        NetworkMonitor.TracingConnectionStateListener listener =
                new NetworkMonitor.TracingConnectionStateListener(tracer, new AtomicBoolean(true));

        listener.onAvailable(true, CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).build());

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData spanData = spans.get(0);
        assertEquals("network.change", spanData.getName());
        Attributes attributes = spanData.getAttributes();
        assertEquals("available", attributes.get(NetworkMonitor.NETWORK_STATUS_KEY));
        assertEquals("wifi", attributes.get(NET_HOST_CONNECTION_TYPE));
        assertNull(attributes.get(NET_HOST_CONNECTION_SUBTYPE));
    }

    @Test
    public void networkAvailable_cellular() {
        NetworkMonitor.TracingConnectionStateListener listener =
                new NetworkMonitor.TracingConnectionStateListener(tracer, new AtomicBoolean(true));

        listener.onAvailable(
                true,
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR).subType("LTE").build());

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData spanData = spans.get(0);
        assertEquals("network.change", spanData.getName());
        Attributes attributes = spanData.getAttributes();
        assertEquals("available", attributes.get(NetworkMonitor.NETWORK_STATUS_KEY));
        assertEquals("cell", attributes.get(NET_HOST_CONNECTION_TYPE));
        assertEquals("LTE", attributes.get(NET_HOST_CONNECTION_SUBTYPE));
    }

    @Test
    public void networkLost() {
        NetworkMonitor.TracingConnectionStateListener listener =
                new NetworkMonitor.TracingConnectionStateListener(tracer, new AtomicBoolean(true));

        listener.onAvailable(
                false, CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build());

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData spanData = spans.get(0);
        assertEquals("network.change", spanData.getName());
        Attributes attributes = spanData.getAttributes();
        assertEquals("lost", attributes.get(NetworkMonitor.NETWORK_STATUS_KEY));
        assertEquals("unavailable", attributes.get(NET_HOST_CONNECTION_TYPE));
        assertNull(attributes.get(NET_HOST_CONNECTION_SUBTYPE));
    }

    @Test
    public void noEventsPlease() {
        AtomicBoolean shouldEmitChangeEvents = new AtomicBoolean(false);

        NetworkMonitor.TracingConnectionStateListener listener =
                new NetworkMonitor.TracingConnectionStateListener(tracer, shouldEmitChangeEvents);

        listener.onAvailable(
                false, CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build());
        assertTrue(otelTesting.getSpans().isEmpty());
        listener.onAvailable(
                true,
                CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR).subType("LTE").build());
        assertTrue(otelTesting.getSpans().isEmpty());

        shouldEmitChangeEvents.set(true);
        listener.onAvailable(
                false, CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build());
        assertEquals(1, otelTesting.getSpans().size());
    }
}
