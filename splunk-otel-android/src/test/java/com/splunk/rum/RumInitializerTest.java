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

import android.app.Application;
import android.os.Looper;

import org.junit.Test;

import java.util.List;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RumInitializerTest {
    @Test
    public void initializationSpan() {
        Config config = mock(Config.class);
        when(config.getBeaconUrl()).thenReturn("http://backend");
        when(config.isCrashReportingEnabled()).thenReturn(true);
        when(config.isAnrDetectionEnabled()).thenReturn(true);
        when(config.isNetworkMonitorEnabled()).thenReturn(true);
        Application application = mock(Application.class);
        InMemorySpanExporter testExporter = InMemorySpanExporter.create();
        RumInitializer testInitializer = new RumInitializer(config, application) {
            @Override
            SpanExporter buildExporter(ConnectionUtil connectionUtil) {
                return testExporter;
            }
        };
        SplunkRum splunkRum = testInitializer.initialize(() -> mock(ConnectionUtil.class, RETURNS_DEEP_STUBS), mock(Looper.class));
        splunkRum.flushSpans();

        List<SpanData> spans = testExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        SpanData initSpan = spans.get(0);
        assertEquals("SplunkRum.initialize", initSpan.getName());
        assertEquals("appstart", initSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertEquals("[debug:false,crashReporting:true,anrReporting:true,networkMonitor:true]",
                initSpan.getAttributes().get(AttributeKey.stringKey("config_settings")));

        List<EventData> events = initSpan.getEvents();
        assertEquals(9, events.size());
        checkEventExists(events, "connectionUtilInitialized");
        checkEventExists(events, "exporterInitialized");
        checkEventExists(events, "sessionIdInitialized");
        checkEventExists(events, "tracerProviderInitialized");
        checkEventExists(events, "openTelemetrySdkInitialized");
        checkEventExists(events, "activityLifecycleCallbacksInitialized");
        checkEventExists(events, "crashReportingInitialized");
        checkEventExists(events, "anrMonitorInitialized");
        checkEventExists(events, "networkMonitorInitialized");
    }

    private void checkEventExists(List<EventData> events, String eventName) {
        assertTrue("Event with name " + eventName + " not found",
                events.stream().map(EventData::getName).anyMatch(name -> name.equals(eventName)));
    }

}