/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.incubator.logs.AnyValue
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.time.Instant
import java.util.function.Consumer
import java.util.function.Supplier

class SdkInitializationEvents(private val clock: Supplier<Instant> = Supplier { Instant.now() }) : InitializationEvents {
    private val events: MutableList<Event> = ArrayList()

    override fun sdkInitializationStarted() {
        addEvent(RumConstants.Events.INIT_EVENT_STARTED)
    }

    override fun recordConfiguration(config: Map<String, String>) {
        val map: MutableMap<String, AnyValue<*>> = HashMap()
        config.entries.forEach(
            Consumer { e: Map.Entry<String, String> ->
                map[e.key] = AnyValue.of(e.value)
            },
        )
        val body = AnyValue.of(map)
        addEvent(RumConstants.Events.INIT_EVENT_CONFIG, body)
    }

    override fun currentNetworkProviderInitialized() {
        addEvent(RumConstants.Events.INIT_EVENT_NET_PROVIDER)
    }

    override fun networkMonitorInitialized() {
        addEvent(RumConstants.Events.INIT_EVENT_NET_MONITOR)
    }

    override fun anrMonitorInitialized() {
        addEvent(RumConstants.Events.INIT_EVENT_ANR_MONITOR)
    }

    override fun slowRenderingDetectorInitialized() {
        addEvent(RumConstants.Events.INIT_EVENT_JANK_MONITOR)
    }

    override fun crashReportingInitialized() {
        addEvent(RumConstants.Events.INIT_EVENT_CRASH_REPORTER)
    }

    override fun spanExporterInitialized(spanExporter: SpanExporter) {
        val attributes =
            Attributes.of(AttributeKey.stringKey("span.exporter"), spanExporter.toString())
        addEvent(RumConstants.Events.INIT_EVENT_SPAN_EXPORTER, attributes)
    }

    fun finish(sdk: OpenTelemetrySdk) {
        val loggerProvider = sdk.sdkLoggerProvider
        val eventLogger =
            SdkEventLoggerProvider.create(loggerProvider).get("otel.initialization.events")
        events.forEach { event: Event ->
            val eventBuilder =
                eventLogger.builder(event.name)
                    .setTimestamp(event.timestamp)
                    .setAttributes(event.attributes)
            if (event.body != null) {
                // TODO: Config is technically correct because config is the only startup event
                // with a body, but this is ultimately clunky/fragile.
                eventBuilder.put("config", event.body)
            }
            eventBuilder.emit()
        }
    }

    private fun addEvent(
        name: String,
        body: AnyValue<*>,
    ) {
        addEvent(name, null, body)
    }

    private fun addEvent(
        name: String,
        attr: Attributes,
    ) {
        addEvent(name, attr, null)
    }

    private fun addEvent(
        name: String,
        attr: Attributes? = null,
        body: AnyValue<*>? = null,
    ) {
        events.add(Event(clock.get(), name, attr, body))
    }

    private class Event(
        val timestamp: Instant,
        val name: String,
        val attributes: Attributes?,
        val body: AnyValue<*>? = null,
    ) {
        private constructor(timestamp: Instant, name: String, body: AnyValue<*>) : this(
            timestamp,
            name,
            null,
            body,
        )
    }
}
