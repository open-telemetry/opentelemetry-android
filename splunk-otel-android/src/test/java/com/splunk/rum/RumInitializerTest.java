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

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.os.Looper;
import com.google.common.base.Strings;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class RumInitializerTest {
    @Test
    public void initializationSpan() {
        Config config =
                Config.builder()
                        .realm("dev")
                        .applicationName("testApp")
                        .rumAccessToken("accessToken")
                        .build();
        Application application = mock(Application.class);
        InMemorySpanExporter testExporter = InMemorySpanExporter.create();
        AppStartupTimer startupTimer = new AppStartupTimer();
        RumInitializer testInitializer =
                new RumInitializer(config, application, startupTimer) {
                    @Override
                    SpanExporter buildFilteringExporter(ConnectionUtil connectionUtil) {
                        return testExporter;
                    }
                };
        SplunkRum splunkRum =
                testInitializer.initialize(
                        () -> mock(ConnectionUtil.class, RETURNS_DEEP_STUBS), mock(Looper.class));
        startupTimer.runCompletionCallback();
        splunkRum.flushSpans();

        List<SpanData> spans = testExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        SpanData initSpan = spans.get(0);
        assertEquals(
                initSpan.getParentSpanContext(), startupTimer.getStartupSpan().getSpanContext());

        assertEquals("SplunkRum.initialize", initSpan.getName());
        assertEquals("appstart", initSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertEquals(
                "[debug:false,crashReporting:true,anrReporting:true,slowRenderingDetector:true,networkMonitor:true]",
                initSpan.getAttributes().get(stringKey("config_settings")));

        List<EventData> events = initSpan.getEvents();
        assertTrue(events.size() > 0);
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
        assertTrue(
                "Event with name " + eventName + " not found",
                events.stream().map(EventData::getName).anyMatch(name -> name.equals(eventName)));
    }

    @Test
    public void spanLimitsAreConfigured() {
        Config config =
                Config.builder()
                        .realm("dev")
                        .applicationName("testApp")
                        .rumAccessToken("accessToken")
                        .build();
        Application application = mock(Application.class);
        InMemorySpanExporter testExporter = InMemorySpanExporter.create();
        AppStartupTimer startupTimer = new AppStartupTimer();
        RumInitializer testInitializer =
                new RumInitializer(config, application, startupTimer) {
                    @Override
                    SpanExporter buildFilteringExporter(ConnectionUtil connectionUtil) {
                        return testExporter;
                    }
                };
        SplunkRum splunkRum =
                testInitializer.initialize(
                        () -> mock(ConnectionUtil.class, RETURNS_DEEP_STUBS), mock(Looper.class));
        splunkRum.flushSpans();

        testExporter.reset();

        AttributeKey<String> longAttributeKey = stringKey("longAttribute");
        splunkRum.addRumEvent(
                "testEvent",
                Attributes.of(
                        longAttributeKey,
                        Strings.repeat("a", RumInitializer.MAX_ATTRIBUTE_LENGTH + 1)));

        splunkRum.flushSpans();
        List<SpanData> spans = testExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());

        SpanData eventSpan = spans.get(0);
        assertEquals("testEvent", eventSpan.getName());
        String truncatedValue = eventSpan.getAttributes().get(longAttributeKey);
        assertEquals(Strings.repeat("a", RumInitializer.MAX_ATTRIBUTE_LENGTH), truncatedValue);
    }

    /** Verify that we have buffering in place in our exporter implementation. */
    @Test
    public void verifyExporterBuffering() {
        Config config =
                Config.builder()
                        .realm("dev")
                        .applicationName("testApp")
                        .rumAccessToken("accessToken")
                        .build();
        Application application = mock(Application.class);
        AppStartupTimer startupTimer = new AppStartupTimer();
        InMemorySpanExporter testExporter = InMemorySpanExporter.create();

        RumInitializer testInitializer =
                new RumInitializer(config, application, startupTimer) {
                    @Override
                    SpanExporter getCoreSpanExporter(String endpoint) {
                        return testExporter;
                    }
                };

        ConnectionUtil connectionUtil = mock(ConnectionUtil.class);

        CurrentNetwork offline = new CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE, null);
        CurrentNetwork online = new CurrentNetwork(NetworkState.TRANSPORT_WIFI, null);
        when(connectionUtil.refreshNetworkStatus()).thenReturn(offline, online);

        long currentTimeNanos = MILLISECONDS.toNanos(System.currentTimeMillis());

        SpanExporter spanExporter = testInitializer.buildFilteringExporter(connectionUtil);
        List<SpanData> batch1 = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            batch1.add(createTestSpan(currentTimeNanos - MINUTES.toNanos(1)));
        }
        // space out the two batches, so they are well under the rate limit
        List<SpanData> batch2 = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            batch2.add(createTestSpan(currentTimeNanos));
        }
        spanExporter.export(batch1);
        spanExporter.export(batch2);

        // we want to verify that everything got exported, including everything buffered while
        // offline.
        assertEquals(198, testExporter.getFinishedSpanItems().size());
    }

    private TestSpanData createTestSpan(long startTimeNanos) {
        return TestSpanData.builder()
                .setName("span")
                .setKind(SpanKind.INTERNAL)
                .setStatus(StatusData.unset())
                .setStartEpochNanos(startTimeNanos)
                .setHasEnded(true)
                .setEndEpochNanos(startTimeNanos)
                .build();
    }

    @Test
    public void shouldTranslateExceptionEventsToSpanAttributes() {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

        Config config =
                Config.builder()
                        .realm("us0")
                        .rumAccessToken("secret!")
                        .applicationName("test")
                        .debugEnabled(true)
                        .build();
        Application application = mock(Application.class);
        ConnectionUtil connectionUtil = mock(ConnectionUtil.class, RETURNS_DEEP_STUBS);
        when(connectionUtil.refreshNetworkStatus().isOnline()).thenReturn(true);

        AppStartupTimer appStartupTimer = new AppStartupTimer();
        RumInitializer initializer =
                new RumInitializer(config, application, appStartupTimer) {
                    @Override
                    SpanExporter getCoreSpanExporter(String endpoint) {
                        return spanExporter;
                    }
                };

        SplunkRum splunkRum = initializer.initialize(() -> connectionUtil, mock(Looper.class));
        appStartupTimer.runCompletionCallback();

        Exception e = new IllegalArgumentException("booom!");
        splunkRum.addRumException(e, Attributes.of(stringKey("attribute"), "oh no!"));
        splunkRum.flushSpans();

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans)
                .satisfiesExactly(
                        span ->
                                OpenTelemetryAssertions.assertThat(span)
                                        .hasName("SplunkRum.initialize"),
                        span ->
                                OpenTelemetryAssertions.assertThat(span)
                                        .hasName("IllegalArgumentException")
                                        .hasAttributesSatisfying(
                                                attributes ->
                                                        OpenTelemetryAssertions.assertThat(
                                                                        attributes)
                                                                .containsEntry(
                                                                        SplunkRum.COMPONENT_KEY,
                                                                        SplunkRum.COMPONENT_ERROR)
                                                                .containsEntry(
                                                                        stringKey("attribute"),
                                                                        "oh no!")
                                                                .containsEntry(
                                                                        SemanticAttributes
                                                                                .EXCEPTION_TYPE,
                                                                        "IllegalArgumentException")
                                                                .containsEntry(
                                                                        SplunkRum.ERROR_TYPE_KEY,
                                                                        "IllegalArgumentException")
                                                                .containsEntry(
                                                                        SemanticAttributes
                                                                                .EXCEPTION_MESSAGE,
                                                                        "booom!")
                                                                .containsEntry(
                                                                        SplunkRum.ERROR_MESSAGE_KEY,
                                                                        "booom!")
                                                                .containsKey(
                                                                        SemanticAttributes
                                                                                .EXCEPTION_STACKTRACE))
                                        .hasEvents(emptyList()));
    }
}
