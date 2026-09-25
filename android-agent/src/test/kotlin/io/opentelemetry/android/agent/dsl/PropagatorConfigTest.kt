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
    fun testAddCustomPropagator() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val customPropagator = mockk<TextMapPropagator>()
        every { customPropagator.fields() } returns listOf("x-custom-header")

        val rum =
            OpenTelemetryRumInitializer.initialize(ctx) {
                addPropagator(customPropagator)
            }
        try {
            val propagator = rum.openTelemetry.propagators.textMapPropagator
            assertThat(propagator.fields()).contains("traceparent", "baggage", "x-custom-header")
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun testMultiplePropagatorsAddedInOrder() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val p1 = mockk<TextMapPropagator>()
        val p2 = mockk<TextMapPropagator>()
        every { p1.fields() } returns listOf("x-header-1")
        every { p2.fields() } returns listOf("x-header-2")

        val rum =
            OpenTelemetryRumInitializer.initialize(ctx) {
                addPropagator(p1)
                addPropagator(p2)
            }
        try {
            val propagator = rum.openTelemetry.propagators.textMapPropagator
            assertThat(propagator.fields()).contains("traceparent", "baggage", "x-header-1", "x-header-2")
            assertThat(propagator.fields()).containsSubsequence("x-header-1", "x-header-2")
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
        val getter = mockk<TextMapGetter<Any>>(relaxed = true)
        every { customPropagator.fields() } returns listOf("x-custom-header")

        every {
            customPropagator.extract(any(), carrier, getter)
        } returns expectedContext

        val rum =
            OpenTelemetryRumInitializer.initialize(ctx) {
                addPropagator(customPropagator)
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
