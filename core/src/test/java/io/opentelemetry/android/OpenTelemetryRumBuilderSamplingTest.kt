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
import io.opentelemetry.sdk.trace.samplers.Sampler
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

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
    fun customizedSamplerIsReadyBeforeInstrumentation() {
        val selectedProvider = SessionProvider { "00000000000000000000000000000001" }
        val instrumentation = mockk<AndroidInstrumentation>(relaxed = true)
        every { instrumentation.name } returns "sampling-test"
        every { instrumentation.install(any(), any()) } answers {
            val rum = secondArg<OpenTelemetryRum>()
            assertThat(rum.sessionProvider).isSameAs(selectedProvider)
            val span = rum.span("installation").startSpan()
            assertThat(span.isRecording).isFalse()
            span.end()
        }
        makeBuilder()
            .setSessionProvider(selectedProvider)
            .addInstrumentation(instrumentation)
            .addTracerProviderCustomizer { builder, _ ->
                builder
                    .setSampler(SessionIdRatioBasedSampler(0.0, selectedProvider))
                    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            }.withRum { rum ->
                verify(exactly = 1) { instrumentation.install(any(), rum) }
                assertThat(spanExporter.finishedSpanItems).isEmpty()
            }
    }

    @Test
    fun tracerCustomizerCanReplaceTraceSampler() {
        makeBuilder()
            .setSessionProvider(SessionProvider { "current" })
            .addTracerProviderCustomizer { builder, _ -> builder.setSampler(Sampler.alwaysOff()) }
            .addTracerProviderCustomizer { builder, _ ->
                builder
                    .setSampler(Sampler.alwaysOn())
                    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            }.withRum { rum ->
                rum.span("kept").startSpan().end()
                rum
                    .span("caller")
                    .setAttribute("session.id", "caller")
                    .startSpan()
                    .end()
                assertThat(spanExporter.finishedSpanItems.map { it.attributes.get(stringKey("session.id")) })
                    .containsExactly("current", "caller")
            }
    }

    @Test
    fun defaultSamplerStillSamplesRootsAndHonorsUnsampledParents() {
        makeBuilder()
            .setSessionProvider(SessionProvider { "current" })
            .addTracerProviderCustomizer { builder, _ -> builder.addSpanProcessor(SimpleSpanProcessor.create(spanExporter)) }
            .withRum { rum ->
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
            }
    }

    @Test
    fun customizedSessionSamplerKeepsTheSessionUsedForSampling() {
        val lowId = "00000000000000000000000000000000"
        val highId = "00000000000000008fffffffffffffff"
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returnsMany listOf(lowId, highId)
        // Wrapping must not change attribution; no checks for a particular sampler class.
        val sampler = object : Sampler by SessionIdRatioBasedSampler(0.5, provider) {}
        makeBuilder()
            .setSessionProvider(provider)
            .addTracerProviderCustomizer { builder, _ ->
                builder.setSampler(sampler).addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            }.withRum { rum ->
                rum
                    .span("sampled")
                    .setAttribute("session.id", "caller")
                    .startSpan()
                    .end()
                rum.span("dropped").startSpan().end()
                assertThat(spanExporter.finishedSpanItems).hasSize(1)
                assertThat(
                    spanExporter.finishedSpanItems
                        .single()
                        .attributes
                        .get(stringKey("session.id")),
                ).isEqualTo(lowId)
                verify(exactly = 2) { provider.getSessionId() }
            }
    }

    @Test
    fun traceSamplingDoesNotFilterLogsOrMetrics() {
        val logs = InMemoryLogRecordExporter.create()
        val metrics = InMemoryMetricReader.create()
        makeBuilder()
            .addTracerProviderCustomizer { builder, _ -> builder.setSampler(Sampler.alwaysOff()) }
            .addLoggerProviderCustomizer { builder, _ ->
                builder.addLogRecordProcessor(SimpleLogRecordProcessor.create(logs))
            }.addMeterProviderCustomizer { builder, _ -> builder.registerMetricReader(metrics) }
            .withRum { rum ->
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
                val point =
                    metrics
                        .collectAllMetrics()
                        .single()
                        .longSumData.points
                        .single()
                assertThat(point.value).isEqualTo(1L)
            }
    }

    private fun OpenTelemetryRumBuilder.withRum(assertions: (OpenTelemetryRum) -> Unit) {
        val rum = build()
        try {
            assertions(rum)
        } finally {
            rum.shutdown()
        }
    }

    private fun OpenTelemetryRum.span(name: String) = openTelemetry.getTracer("test").spanBuilder(name)

    private fun makeBuilder(): OpenTelemetryRumBuilder = OpenTelemetryRumBuilder(application, buildConfig())

    private fun buildConfig(): OtelRumConfig =
        OtelRumConfig()
            .disableNetworkAttributes()
            .disableScreenAttributes()
            .disableSdkInitializationEvents()
            .disableInstrumentationDiscovery()
}
