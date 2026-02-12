/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.sdk.trace.IdGenerator
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class OtelSdkCustomizationsSpecTest {
    @Test
    fun `can configure tracer provider`() {
        val traceId = "b9a654847cb3514b6e5bd80cb168ed1c"
        val spanId = "0666666666666666"
        val idGen =
            object : IdGenerator {
                override fun generateSpanId(): String? = spanId

                override fun generateTraceId(): String? = traceId
            }
        val agent =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    otelSdkCustomizations {
                        customizeTracerProvider { _ ->
                            SdkTracerProvider.builder().setIdGenerator(idGen)
                        }
                    }
                },
            )
        val tracer =
            agent.openTelemetry.tracerProvider
                .tracerBuilder("test")
                .build()
        val span = tracer.spanBuilder("test").startSpan()
        assertThat(span.spanContext.spanId).isEqualTo(spanId)
    }

    @Test
    fun `can configure logger provider`() {
        val seen = AtomicBoolean(false)
        val agent =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    otelSdkCustomizations {
                        customizeLoggerProvider { builder ->
                            builder.addLogRecordProcessor { _, _ ->
                                run {
                                    seen.set(true)
                                }
                            }
                        }
                    }
                },
            )
        val logger =
            agent.openTelemetry.logsBridge
                .loggerBuilder("test")
                .build()
        logger.logRecordBuilder().setBody("howdy").emit()
        assertThat(seen.get()).isTrue
    }

    @Test
    fun `can configure meter provider`() {
        val seen = AtomicBoolean(false)
        val agent =
            OpenTelemetryRumInitializer.initialize(
                context = RuntimeEnvironment.getApplication(),
                configuration = {
                    otelSdkCustomizations {
                        customizeMeterProvider { builder ->
                            run {
                                seen.set(true)
                                builder
                            }
                        }
                    }
                },
            )
        val counter =
            agent.openTelemetry.meterProvider
                .get("test")
                .counterBuilder("test")
                .build()
        counter.add(1)
        assertThat(seen.get()).isTrue
    }
}
