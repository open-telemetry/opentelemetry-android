/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup;

import static io.opentelemetry.android.common.RumConstants.Events.INIT_EVENT_ANR_MONITOR;
import static io.opentelemetry.android.common.RumConstants.Events.INIT_EVENT_CONFIG;
import static io.opentelemetry.android.common.RumConstants.Events.INIT_EVENT_CRASH_REPORTER;
import static io.opentelemetry.android.common.RumConstants.Events.INIT_EVENT_JANK_MONITOR;
import static io.opentelemetry.android.common.RumConstants.Events.INIT_EVENT_NET_MONITOR;
import static io.opentelemetry.android.common.RumConstants.Events.INIT_EVENT_NET_PROVIDER;
import static io.opentelemetry.android.common.RumConstants.Events.INIT_EVENT_SPAN_EXPORTER;
import static io.opentelemetry.android.common.RumConstants.Events.INIT_EVENT_STARTED;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.events.EventBuilder;
import io.opentelemetry.api.incubator.events.EventLogger;
import io.opentelemetry.api.incubator.logs.AnyValue;
import io.opentelemetry.api.incubator.logs.KeyAnyValue;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class SdkInitializationEvents implements InitializationEvents {

    private final List<Event> events = new ArrayList<>();
    private final Supplier<Instant> clock = Instant::now;

    @Override
    public void sdkInitializationStarted() {
       addEvent(INIT_EVENT_STARTED);
    }

    @Override
    public void recordConfiguration(Map<String, String> config) {
        Map<String,AnyValue<?>> map = new HashMap<>();
        config.entrySet().forEach(e -> map.put(e.getKey(), AnyValue.of(e.getValue())));
        AnyValue<List<KeyAnyValue>> body = AnyValue.of(map);
        addEvent(INIT_EVENT_CONFIG, body);
    }

    @Override
    public void currentNetworkProviderInitialized() {
        addEvent(INIT_EVENT_NET_PROVIDER);
    }

    @Override
    public void networkMonitorInitialized() {
        addEvent(INIT_EVENT_NET_MONITOR);
    }

    @Override
    public void anrMonitorInitialized() {
        addEvent(INIT_EVENT_ANR_MONITOR);
    }

    @Override
    public void slowRenderingDetectorInitialized() {
        addEvent(INIT_EVENT_JANK_MONITOR);
    }

    @Override
    public void crashReportingInitialized() {
        addEvent(INIT_EVENT_CRASH_REPORTER);
    }

    @Override
    public void spanExporterInitialized(SpanExporter spanExporter) {
        Attributes attributes = Attributes.of(stringKey("span.exporter"), spanExporter.toString());
        addEvent(INIT_EVENT_SPAN_EXPORTER, attributes);
    }

    public void finish(OpenTelemetrySdk sdk) {
        SdkLoggerProvider loggerProvider = sdk.getSdkLoggerProvider();
        EventLogger eventLogger = SdkEventLoggerProvider.create(loggerProvider).get("otel.initialization.events");
        events.forEach(event -> {
            EventBuilder eventBuilder = eventLogger.builder(event.name)
                    .setTimestamp(event.timestamp)
                    .setAttributes(event.attributes);
            if(event.body != null){
                // TODO: Config is technically correct because config is the only startup event
                // with a body, but this is ultimately clunky/fragile.
                eventBuilder.put("config", event.body);
            }
            eventBuilder.emit();
        });
    }

    private void addEvent(String name){
        addEvent(name, null, null);
    }
    private void addEvent(String name, AnyValue<?> body){
        addEvent(name, null, body);
    }

    private void addEvent(String name, Attributes attr){
        addEvent(name, attr);
    }
    private void addEvent(String name, Attributes attr, AnyValue<?> body){
        events.add(new Event(clock.get(), name, attr, body));
    }

    private static class Event {
        private final Instant timestamp;
        private final String name;
        private final Attributes attributes;
        private final AnyValue<?> body;

        private Event(Instant timestamp, String name, Attributes attributes) {
            this(timestamp, name, attributes, null);
        }

        private Event(Instant timestamp, String name, AnyValue<?> body) {
            this(timestamp, name, null, body);
        }
        private Event(Instant timestamp, String name, Attributes attributes, AnyValue<?> body) {
            this.timestamp = timestamp;
            this.name = name;
            this.attributes = attributes;
            this.body = body;
        }
    }
}
