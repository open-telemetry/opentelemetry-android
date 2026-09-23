/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import io.opentelemetry.context.Context as OtelContext

@RunWith(AndroidJUnit4::class)
class PropagatorConfigTest {
    @Test
    fun testDefaultPropagators() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val rum = OpenTelemetryRumInitializer.initialize(ctx)
        try {
            val propagator = rum.openTelemetry.propagators.textMapPropagator
            assertThat(propagator.fields()).contains("traceparent", "baggage")
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun testSetCustomPropagator() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val customPropagator = mockk<TextMapPropagator>()

        val rum =
            OpenTelemetryRumInitializer.initialize(ctx) {
                addPropagatorCustomizer { customPropagator }
            }
        try {
            val result = rum.openTelemetry.propagators.textMapPropagator
            assertThat(result).isSameAs(customPropagator)
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun testAddPropagatorComposite() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val customPropagator = mockk<TextMapPropagator>()
        every { customPropagator.fields() } returns listOf("x-custom-header")

        val rum =
            OpenTelemetryRumInitializer.initialize(ctx) {
                addPropagatorCustomizer { existing ->
                    TextMapPropagator.composite(existing, customPropagator)
                }
            }
        try {
            val propagator = rum.openTelemetry.propagators.textMapPropagator
            assertThat(propagator.fields()).contains("traceparent", "baggage", "x-custom-header")
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun testMultiplePropagatorCustomizersExecutedInOrder() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val p1 = mockk<TextMapPropagator>()
        val p2 = mockk<TextMapPropagator>()

        val rum =
            OpenTelemetryRumInitializer.initialize(ctx) {
                addPropagatorCustomizer { _ -> p1 }
                addPropagatorCustomizer { existing ->
                    assertThat(existing).isSameAs(p1)
                    p2
                }
            }
        try {
            val result = rum.openTelemetry.propagators.textMapPropagator
            assertThat(result).isSameAs(p2)
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun testCustomPropagatorExtract() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val otelContext = OtelContext.root()
        val carrier = Any()
        val expectedContext = mockk<OtelContext>()
        val customPropagator = mockk<TextMapPropagator>()
        val getter = mockk<TextMapGetter<Any>>()

        every {
            customPropagator.extract(otelContext, carrier, getter)
        } returns expectedContext

        val rum =
            OpenTelemetryRumInitializer.initialize(ctx) {
                addPropagatorCustomizer { customPropagator }
            }
        try {
            val result =
                rum.openTelemetry.propagators.textMapPropagator
                    .extract(otelContext, carrier, getter)
            assertThat(result).isSameAs(expectedContext)
        } finally {
            rum.shutdown()
        }
    }
}
