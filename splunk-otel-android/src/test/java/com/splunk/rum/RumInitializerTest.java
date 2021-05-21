package com.splunk.rum;

import android.app.Application;

import org.junit.Test;

import java.util.List;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RumInitializerTest {
    @Test
    public void initializationSpan() {
        Config config = mock(Config.class);
        when(config.getBeaconUrl()).thenReturn("http://backend");
        when(config.isCrashReportingEnabled()).thenReturn(true);
        Application application = mock(Application.class);
        InMemorySpanExporter testExporter = InMemorySpanExporter.create();
        RumInitializer testInitializer = new RumInitializer(config, application) {
            @Override
            SpanExporter buildExporter() {
                return testExporter;
            }
        };
        SplunkRum splunkRum = testInitializer.initialize();
        splunkRum.flushSpans();

        List<SpanData> spans = testExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        SpanData initSpan = spans.get(0);
        assertEquals("RUM initialization", initSpan.getName());
        assertEquals("app", initSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));

        List<EventData> events = initSpan.getEvents();
        assertEquals(6, events.size());
        checkEventExists(events, "exporterInitialized");
        checkEventExists(events, "sessionIdInitialized");
        checkEventExists(events, "tracerProviderInitialized");
        checkEventExists(events, "openTelemetrySdkInitialized");
        checkEventExists(events, "activityLifecycleCallbacksInitialized");
        checkEventExists(events, "crashReportingInitialized");
    }

    private void checkEventExists(List<EventData> events, String eventName) {
        assertTrue("Event with name " + eventName + " not found",
                events.stream().map(EventData::getName).anyMatch(name -> name.equals(eventName)));
    }

}