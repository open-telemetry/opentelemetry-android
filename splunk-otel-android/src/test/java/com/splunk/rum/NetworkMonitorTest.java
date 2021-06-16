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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NetworkMonitorTest {
    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }


    @Test
    public void networkAvailable_wifi() {
        NetworkMonitor.TracingConnectionStateListener listener = new NetworkMonitor.TracingConnectionStateListener(tracer);

        listener.onAvailable(true, new CurrentNetwork(NetworkState.TRANSPORT_WIFI, null));

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData spanData = spans.get(0);
        assertEquals("network.change", spanData.getName());
        Attributes attributes = spanData.getAttributes();
        assertEquals("available", attributes.get(NetworkMonitor.NETWORK_STATUS_KEY));
        assertEquals("WIFI", attributes.get(RumAttributeAppender.NETWORK_TYPE_KEY));
        assertNull(attributes.get(RumAttributeAppender.NETWORK_SUBTYPE_KEY));
    }

    @Test
    public void networkAvailable_cellular() {
        NetworkMonitor.TracingConnectionStateListener listener = new NetworkMonitor.TracingConnectionStateListener(tracer);

        listener.onAvailable(true, new CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, "LTE"));

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData spanData = spans.get(0);
        assertEquals("network.change", spanData.getName());
        Attributes attributes = spanData.getAttributes();
        assertEquals("available", attributes.get(NetworkMonitor.NETWORK_STATUS_KEY));
        assertEquals("CELLULAR", attributes.get(RumAttributeAppender.NETWORK_TYPE_KEY));
        assertEquals("LTE", attributes.get(RumAttributeAppender.NETWORK_SUBTYPE_KEY));
    }

    @Test
    public void networkLost() {
        NetworkMonitor.TracingConnectionStateListener listener = new NetworkMonitor.TracingConnectionStateListener(tracer);

        listener.onAvailable(false, new CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE, null));

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData spanData = spans.get(0);
        assertEquals("network.change", spanData.getName());
        Attributes attributes = spanData.getAttributes();
        assertEquals("lost", attributes.get(NetworkMonitor.NETWORK_STATUS_KEY));
        assertEquals("NONE", attributes.get(RumAttributeAppender.NETWORK_TYPE_KEY));
        assertNull(attributes.get(RumAttributeAppender.NETWORK_SUBTYPE_KEY));
    }
}