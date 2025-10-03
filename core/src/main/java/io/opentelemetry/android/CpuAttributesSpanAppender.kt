/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.os.Process
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.internal.ExtendedSpanProcessor

/**
 * A [SpanProcessor] that uses the experimental ExtendedSpanProcessor API to append OS process
 * cpu statistics into span attributes. We establish 'cpu utilization average' to be:
 *
 *   cpuUtilizationAvg = 100 * (cpuTimeMs / spanDurationMs) / number of CPU cores
 *     * cpuTimeMs is the time in milliseconds that the app process has taken in active CPU
 *       time
 *     * spanDurationMs is the total running time in milliseconds that the span has been active
 *       for
 */
class CpuAttributesSpanAppender(
    private val cpuCores: Int = Runtime.getRuntime().availableProcessors(),
) : ExtendedSpanProcessor {
    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {}

    override fun isEndRequired(): Boolean = false

    override fun isOnEndingRequired(): Boolean = true

    override fun onStart(
        parentContext: Context,
        span: ReadWriteSpan,
    ) {
        val cputime = Process.getElapsedCpuTime()
        span.setAttribute(RumConstants.CPU_ELAPSED_TIME_START_KEY, Process.getElapsedCpuTime())
    }

    override fun onEnding(span: ReadWriteSpan) {
        val startCpuTime =
            span.getAttribute(RumConstants.CPU_ELAPSED_TIME_START_KEY) ?: return
        val endCpuTime = Process.getElapsedCpuTime()
        val cpuTimeMs = (endCpuTime - startCpuTime).toDouble()
        val spanDurationMs = (span.latencyNanos / 1_000_000).toDouble()

        if (spanDurationMs > 0) {
            val cpuUtilization = (cpuTimeMs / spanDurationMs) * 100.0 / cpuCores.toDouble()
            span.setAttribute(RumConstants.CPU_AVERAGE_KEY, cpuUtilization)
        }
        span.setAttribute(RumConstants.CPU_ELAPSED_TIME_END_KEY, endCpuTime)
    }
}
