/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.coroutines.testing

import io.opentelemetry.android.test.common.OpenTelemetryRumRule
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.library.coroutines.CoroutinesTestUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class InstrumentationTest {
    @Rule
    @JvmField
    var otelRule: OpenTelemetryRumRule = OpenTelemetryRumRule()

    private val scope = CoroutineScope(Dispatchers.IO)

    @Test
    fun test_launch_default_propagates_context_after_suspension() {
        val tracer = otelRule.openTelemetryRum.openTelemetry.getTracer("test")
        val parentSpan = tracer.spanBuilder("parent").startSpan()
        val latch = CountDownLatch(1)
        var spanIdInCoroutine: String? = null

        parentSpan.makeCurrent().use {
            CoroutinesTestUtil.launchDefault(scope) {
                delay(10)
                spanIdInCoroutine = Span.current().spanContext.spanId
                latch.countDown()
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(spanIdInCoroutine).isEqualTo(parentSpan.spanContext.spanId)
        assertThat(Context.current()).isEqualTo(Context.root())
        parentSpan.end()
    }

    @Test
    fun test_launch_direct_propagates_context_after_suspension() {
        val tracer = otelRule.openTelemetryRum.openTelemetry.getTracer("test")
        val parentSpan = tracer.spanBuilder("parent").startSpan()
        val latch = CountDownLatch(1)
        var spanIdInCoroutine: String? = null

        parentSpan.makeCurrent().use {
            CoroutinesTestUtil.launchDirect(scope) {
                delay(10)
                spanIdInCoroutine = Span.current().spanContext.spanId
                latch.countDown()
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(spanIdInCoroutine).isEqualTo(parentSpan.spanContext.spanId)
        assertThat(Context.current()).isEqualTo(Context.root())
        parentSpan.end()
    }

    @Test
    fun test_no_context_when_no_span_active() {
        val latch = CountDownLatch(1)
        var spanInsideCoroutine: Span? = null

        CoroutinesTestUtil.launchDefault(scope) {
            delay(10)
            spanInsideCoroutine = Span.current()
            latch.countDown()
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(spanInsideCoroutine!!.spanContext.isValid).isFalse()
    }

    @Test
    fun test_no_context_leakage_to_caller_thread() {
        val tracer = otelRule.openTelemetryRum.openTelemetry.getTracer("test")
        val parentSpan = tracer.spanBuilder("parent").startSpan()
        val latch = CountDownLatch(1)

        parentSpan.makeCurrent().use {
            CoroutinesTestUtil.launchDefault(scope) {
                delay(10)
                latch.countDown()
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(Span.current().spanContext.isValid).isFalse()
        parentSpan.end()
    }
}
