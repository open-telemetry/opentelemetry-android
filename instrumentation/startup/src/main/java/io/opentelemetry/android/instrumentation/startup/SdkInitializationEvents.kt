/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import com.google.auto.service.AutoService
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.android.ktx.setSessionIdentifiersWith
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.Value
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

@AutoService(InitializationEvents::class)
class SdkInitializationEvents(
    private val clock: Supplier<Instant> = Supplier { Instant.now() },
) : InitializationEvents {
    private val events = ConcurrentLinkedQueue<Event>()
    private val eventLogger = AtomicReference<Logger?>()
    private val sessionProvider = AtomicReference<SessionProvider?>()

    override fun sdkInitializationStarted() {
        addEvent(RumConstants.Events.INIT_EVENT_STARTED)
    }

    override fun recordConfiguration(config: OtelRumConfig) {
        // TODO convert config to AnyValue and add an event for it named RumConstants.Events.INIT_EVENT_CONFIG
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
        addEvent(RumConstants.Events.INIT_EVENT_SPAN_EXPORTER, attr = attributes)
    }

    internal fun finish(
        openTelemetry: OpenTelemetry,
        provider: SessionProvider,
    ) {
        val logger = openTelemetry.logsBridge.loggerBuilder("otel.initialization.events").build()
        eventLogger.set(logger)
        sessionProvider.set(provider)
        logger.emitInitEvents()
    }

    private fun Logger.emitInitEvents() {
        do {
            events.poll()?.emit(this)
        } while (events.isNotEmpty())
    }

    private fun Event.emit(logger: Logger) {
        val eventBuilder = logger.logRecordBuilder()
        val provider = sessionProvider.get()
        eventBuilder
            .apply {
                if (provider != null) {
                    setSessionIdentifiersWith(provider)
                }
            }.setEventName(name)
            .setTimestamp(timestamp)
            .apply {
                if (attributes != null) {
                    setAllAttributes(attributes)
                }
            }.emit()
    }

    /**
     * Add an init event with the given name and attributes. A log will be recorded synchronously if the logger has already been
     * initialized (when [finish] is called), or it will be queued up for emission later when [finish] actually gets called.
     */
    private fun addEvent(
        name: String,
        attr: Attributes? = null,
    ) {
        events.add(Event(clock.get(), name, attr))
        eventLogger.get()?.emitInitEvents()
    }

    private data class Event(
        val timestamp: Instant,
        val name: String,
        val attributes: Attributes?,
        val body: Value<*>? = null,
    )
}
