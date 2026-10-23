/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.opentelemetry.android.SessionIdRatioBasedSampler
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceId
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.Random

class SessionIdRatioBasedPredicateTest {
    @Test
    fun `matches the upstream algorithm and existing sampler description`() {
        val random = Random(970)
        val ids = List(100) { TraceId.fromLongs(random.nextLong(), random.nextLong()) } + listOf("", "invalid")
        listOf(0.0, 0.01, 0.5, 0.99, 1.0).forEach { ratio ->
            val upstream = Sampler.traceIdRatioBased(ratio)
            val predicate = SessionIdRatioBasedPredicate(ratio)
            ids.forEach { id ->
                val expected =
                    runCatching {
                        upstream.shouldSample(Context.root(), id, "", SpanKind.INTERNAL, Attributes.empty(), emptyList()).decision
                    }
                val actual = runCatching { predicate.test(id) }
                assertThat(actual.getOrNull()).isEqualTo(expected.getOrNull()?.let { it == SamplingDecision.RECORD_AND_SAMPLE })
                assertThat(actual.exceptionOrNull()?.javaClass).isEqualTo(expected.exceptionOrNull()?.javaClass)
                val sampler = SessionIdRatioBasedSampler(ratio) { id }
                val sampled =
                    runCatching {
                        sampler.shouldSample(Context.root(), "", "", SpanKind.INTERNAL, Attributes.empty(), emptyList()).decision
                    }
                assertThat(sampled.getOrNull()).isEqualTo(expected.getOrNull())
                assertThat(sampled.exceptionOrNull()?.javaClass).isEqualTo(expected.exceptionOrNull()?.javaClass)
                assertThat(sampler.description).isEqualTo("SessionIdRatioBased{traceIdRatioBased:$upstream.description}")
            }
        }
    }

    @Test
    fun `session decision does not depend on the current span`() {
        val predicate = SessionIdRatioBasedPredicate(0.5)
        val contexts =
            listOf(Context.root()) +
                listOf(TraceFlags.getSampled(), TraceFlags.getDefault()).map { flags ->
                    Context.root().with(
                        Span.wrap(
                            SpanContext.create(
                                "00000000000000000000000000000002",
                                "0000000000000001",
                                flags,
                                TraceState.getDefault(),
                            ),
                        ),
                    )
                }
        contexts.forEach { context ->
            context.makeCurrent().use {
                assertThat(predicate.test("00000000000000000000000000000001")).isTrue()
                assertThat(predicate.test("00000000000000007fffffffffffffff")).isFalse()
            }
        }
    }

    @Test
    fun `rejects out of range ratios like the upstream sampler`() {
        listOf(-0.01, 1.01).forEach { ratio ->
            assertThatThrownBy { SessionIdRatioBasedPredicate(ratio) }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
