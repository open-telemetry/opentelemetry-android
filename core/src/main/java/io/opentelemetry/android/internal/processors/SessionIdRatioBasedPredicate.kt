/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import java.util.function.Predicate

/** Deterministic only for a fixed ratio; this does not retain decisions across configuration changes. */
internal class SessionIdRatioBasedPredicate(
    ratio: Double,
) : Predicate<String> {
    private val sampler = Sampler.traceIdRatioBased(ratio)

    // The session decision must not depend on the active span's sampling decision.
    override fun test(sessionId: String): Boolean =
        sampler.shouldSample(Context.root(), sessionId, "", SpanKind.INTERNAL, Attributes.empty(), emptyList()).decision ==
            SamplingDecision.RECORD_AND_SAMPLE

    override fun toString(): String = sampler.toString()
}
