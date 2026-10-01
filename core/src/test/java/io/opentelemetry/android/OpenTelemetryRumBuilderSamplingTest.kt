/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context.root
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.sdk.trace.samplers.Sampler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import java.util.function.Function

@RunWith(AndroidJUnit4::class)
class OpenTelemetryRumBuilderSamplingTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val spanExporter = InMemorySpanExporter.create()

    @Before
    fun setup() {
        Services.set(mockk(relaxed = true))
        InitializationEvents.set(mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        Services.set(null)
        InitializationEvents.resetForTest()
        clearAllMocks()
    }

    @Test
    fun sessionSamplerReceivesFinalProviderBeforeInstrumentation() {
        val selectedProvider = SessionProvider { "00000000000000000000000000000001" }
        val factory = mockk<Function<SessionProvider, Sampler>>()
        every { factory.apply(selectedProvider) } returns SessionIdRatioBasedSampler(0.0, selectedProvider)
        val instrumentation = mockk<AndroidInstrumentation>(relaxed = true)
        every { instrumentation.name } returns "sampling-test"
        every { instrumentation.install(any(), any()) } answers {
            verify(exactly = 1) { factory.apply(selectedProvider) }
            val rum = secondArg<OpenTelemetryRum>()
            assertThat(rum.sessionProvider).isSameAs(selectedProvider)
            val span =
                rum.openTelemetry
                    .getTracer("test")
                    .spanBuilder("installation")
                    .startSpan()
            assertThat(span.isRecording).isFalse()
            span.end()
        }
        val rum =
            makeBuilder()
                .setSessionProvider(SessionProvider { "replaced" })
                .setSessionSampler(factory)
                .setSessionProvider(selectedProvider)
                .addInstrumentation(instrumentation)
                .addTracerProviderCustomizer { builder, _ ->
                    builder.addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                }.build()
        try {
            verify(exactly = 1) { instrumentation.install(any(), rum) }
            verify(exactly = 1) { factory.apply(selectedProvider) }
            assertThat(spanExporter.finishedSpanItems).isEmpty()
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun tracerCustomizerCanReplaceSessionSampler() {
        val rum =
            makeBuilder()
                .addTracerProviderCustomizer { builder, _ ->
                    builder
                        .setSampler(Sampler.alwaysOn())
                        .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                }.setSessionSampler { Sampler.alwaysOff() }
                .build()
        try {
            rum.openTelemetry
                .getTracer("test")
                .spanBuilder("kept")
                .startSpan()
                .end()
            assertThat(spanExporter.finishedSpanItems).hasSize(1)
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun sessionSamplerReceivesNoopProviderAndLastFactoryWins() {
        val unused = mockk<Function<SessionProvider, Sampler>>()
        val factory = mockk<Function<SessionProvider, Sampler>>()
        every { factory.apply(SessionProvider.getNoop()) } returns Sampler.alwaysOn()
        val rum = makeBuilder().setSessionSampler(unused).setSessionSampler(factory).build()
        try {
            verify(exactly = 0) { unused.apply(any()) }
            verify(exactly = 1) { factory.apply(SessionProvider.getNoop()) }
            val span =
                rum.openTelemetry
                    .getTracer("test")
                    .spanBuilder("noop-provider")
                    .startSpan()
            assertThat(span.isRecording).isTrue()
            span.end()
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun disabledTracingDoesNotCallSessionSamplerFactory() {
        val factory = mockk<Function<SessionProvider, Sampler>>()
        val rum =
            OpenTelemetryRumBuilder(application, buildConfig().setTracingDisabled())
                .setSessionSampler(factory)
                .build()
        try {
            val span =
                rum.openTelemetry
                    .getTracer("test")
                    .spanBuilder("disabled")
                    .startSpan()
            assertThat(span.isRecording).isFalse()
            span.end()
            verify(exactly = 0) { factory.apply(any()) }
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun sessionSamplerFactoryFailureAbortsBeforeExportersAndInstrumentation() {
        val failure = IllegalArgumentException("invalid sampling configuration")
        val exporterFactory = mockk<Function<SpanExporter, SpanExporter>>()
        val instrumentation = mockk<AndroidInstrumentation>(relaxed = true)
        val builder =
            makeBuilder()
                .setSessionSampler { throw failure }
                .addTracerProviderCustomizer { _, _ -> error("Customizer must not run") }
                .addSpanExporterCustomizer(exporterFactory)
                .addInstrumentation(instrumentation)
        assertThatThrownBy { builder.build() }.isSameAs(failure)
        verify(exactly = 0) { exporterFactory.apply(any()) }
        verify(exactly = 0) { instrumentation.install(any(), any()) }
    }

    @Test
    fun nullSessionSamplerFailsBeforeCustomizers() {
        // Java factories can return null despite the Kotlin signature.
        @Suppress("UNCHECKED_CAST")
        val factory = Function<SessionProvider, Sampler?> { null } as Function<SessionProvider, Sampler>
        val builder =
            makeBuilder()
                .setSessionSampler(factory)
                .addTracerProviderCustomizer { _, _ -> error("Customizer must not run") }
        assertThatThrownBy { builder.build() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Session sampler factory returned null")
    }

    @Test
    fun defaultSamplerStillSamplesRootsAndHonorsUnsampledParents() {
        val rum =
            makeBuilder()
                .setSessionProvider(SessionProvider { "current" })
                .addTracerProviderCustomizer { builder, _ -> builder.addSpanProcessor(SimpleSpanProcessor.create(spanExporter)) }
                .build()
        try {
            val tracer = rum.openTelemetry.getTracer("test")
            val span =
                tracer
                    .spanBuilder("root")
                    .setNoParent()
                    .setAttribute("session.id", "caller")
                    .startSpan()
            assertThat(span.spanContext.isSampled).isTrue()
            span.end()
            val attributes = spanExporter.finishedSpanItems.single().attributes
            assertThat(attributes.get(stringKey("session.id"))).isEqualTo("current")
            val parent =
                Span.wrap(
                    SpanContext.createFromRemoteParent(
                        "00000000000000000000000000000001",
                        "0000000000000001",
                        TraceFlags.getDefault(),
                        TraceState.getDefault(),
                    ),
                )
            val child = tracer.spanBuilder("child").setParent(root().with(parent)).startSpan()
            assertThat(child.isRecording).isFalse()
            child.end()
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun customizerPreservesCallerSessionIdOnlyWithFactory() {
        for (configureFactory in listOf(false, true)) {
            val exporter = InMemorySpanExporter.create()
            val builder = makeBuilder().setSessionProvider(SessionProvider { "current" })
            if (configureFactory) builder.setSessionSampler { Sampler.alwaysOff() }
            val rum =
                builder
                    .addTracerProviderCustomizer { provider, _ ->
                        provider.setSampler(Sampler.alwaysOn()).addSpanProcessor(SimpleSpanProcessor.create(exporter))
                    }.build()
            try {
                rum.openTelemetry
                    .getTracer("test")
                    .spanBuilder("explicit")
                    .setAttribute("session.id", "caller")
                    .startSpan()
                    .end()
                val attributes = exporter.finishedSpanItems.single().attributes
                assertThat(attributes.get(stringKey("session.id"))).isEqualTo(if (configureFactory) "caller" else "current")
            } finally {
                rum.shutdown()
            }
        }
    }

    @Test
    fun traceSamplingDoesNotFilterLogsOrMetrics() {
        val logs = InMemoryLogRecordExporter.create()
        val metrics = InMemoryMetricReader.create()
        val rum =
            makeBuilder()
                .setSessionSampler { Sampler.alwaysOff() }
                .addLoggerProviderCustomizer { builder, _ ->
                    builder.addLogRecordProcessor(SimpleLogRecordProcessor.create(logs))
                }.addMeterProviderCustomizer { builder, _ -> builder.registerMetricReader(metrics) }
                .build()
        try {
            rum.openTelemetry.logsBridge
                .get("test")
                .logRecordBuilder()
                .setBody("still recorded")
                .emit()
            rum.openTelemetry
                .getMeter("test")
                .counterBuilder("counter")
                .build()
                .add(1)
            assertThat(logs.finishedLogRecordItems).hasSize(1)
            assertThat(
                metrics
                    .collectAllMetrics()
                    .single()
                    .longSumData.points
                    .single()
                    .value,
            ).isEqualTo(1L)
        } finally {
            rum.shutdown()
        }
    }

    private fun makeBuilder(): OpenTelemetryRumBuilder = OpenTelemetryRumBuilder(application, buildConfig())

    private fun buildConfig(): OtelRumConfig =
        OtelRumConfig()
            .disableNetworkAttributes()
            .disableScreenAttributes()
            .disableSdkInitializationEvents()
            .disableInstrumentationDiscovery()
}
