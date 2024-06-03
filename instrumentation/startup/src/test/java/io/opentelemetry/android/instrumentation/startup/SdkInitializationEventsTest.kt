/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.incubator.logs.AnyValue
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.Body
import io.opentelemetry.sdk.logs.internal.AnyValueBody
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.function.Supplier

class SdkInitializationEventsTest {
    @Test
    fun `test all events`() {
        val now = System.currentTimeMillis()
        val fakeTime = AtomicLong(now)
        val clock: Supplier<Instant> =
            Supplier {
                Instant.ofEpochMilli(fakeTime.getAndAdd(50))
            }
        val config = mapOf(Pair("foo", "bar"), Pair("bar", "baz"))
        val seen: MutableList<ReadWriteLogRecord> = ArrayList()
        val expectedConfig =
            AnyValueBody.create(AnyValue.of(config.mapValues { AnyValue.of(it.value) }))

        val exporter = mockk<SpanExporter>()
        val processor = mockk<LogRecordProcessor>()
        val loggerProvider =
            SdkLoggerProvider.builder()
                .addLogRecordProcessor(processor)
                .build()
        val sdk =
            OpenTelemetrySdk.builder()
                .setLoggerProvider(loggerProvider)
                .build()
        every { processor.onEmit(any(), any()) }.answers {
            seen.add(it.invocation.args[1] as ReadWriteLogRecord)
        }
        every { exporter.toString() }.returns("com.cool.Exporter")

        val events = SdkInitializationEvents(clock)

        events.sdkInitializationStarted()
        events.anrMonitorInitialized()
        events.crashReportingInitialized()
        events.currentNetworkProviderInitialized()
        events.networkMonitorInitialized()
        events.recordConfiguration(config)
        events.slowRenderingDetectorInitialized()
        events.spanExporterInitialized(exporter)

        verify { listOf(processor) wasNot called }
        verify(exactly = 0) { exporter.export(any()) }

        events.finish(sdk)

        assertThat(seen).satisfiesExactly(
            time(now).named(RumConstants.Events.INIT_EVENT_STARTED),
            time(now + 50).named(RumConstants.Events.INIT_EVENT_ANR_MONITOR),
            time(now + 100).named(RumConstants.Events.INIT_EVENT_CRASH_REPORTER),
            time(now + 150).named(RumConstants.Events.INIT_EVENT_NET_PROVIDER),
            time(now + 200).named(RumConstants.Events.INIT_EVENT_NET_MONITOR),
            time(now + 250).named(RumConstants.Events.INIT_EVENT_CONFIG).withBody(expectedConfig),
            time(now + 300).named(RumConstants.Events.INIT_EVENT_JANK_MONITOR),
            time(now + 350).named(RumConstants.Events.INIT_EVENT_SPAN_EXPORTER).withAttributes(
                "span.exporter",
                "com.cool.Exporter",
            ),
        )
    }

    fun time(timeMs: Long): EventAssert {
        return EventAssert(TimeUnit.MILLISECONDS.toNanos(timeMs))
    }

    class EventAssert(val timeNs: Long) : Consumer<ReadWriteLogRecord> {
        lateinit var name: String
        var body: Body? = null
        var attrs: Attributes? = null

        override fun accept(log: ReadWriteLogRecord) {
            val logData = log.toLogRecordData()
            assertThat(logData.timestampEpochNanos).isEqualTo(timeNs)
            assertThat(logData.attributes.get(stringKey("event.name"))).isEqualTo(name)
            if (body == null) {
                assertThat(logData.body.type).isEqualTo(Body.Type.EMPTY)
            } else {
                assertThat(logData.body.type).isNotEqualTo(Body.Type.EMPTY)
            }
            if (attrs != null) {
                assertThat(logData.attributes).isEqualTo(attrs)
            }
        }

        fun named(name: String): EventAssert {
            this.name = name
            return this
        }

        fun withBody(body: Body): EventAssert {
            this.body = body
            return this
        }

        fun withAttributes(
            key: String,
            value: String,
        ): EventAssert {
            attrs = Attributes.of(stringKey("event.name"), name, stringKey(key), value)
            return this
        }
    }
}
