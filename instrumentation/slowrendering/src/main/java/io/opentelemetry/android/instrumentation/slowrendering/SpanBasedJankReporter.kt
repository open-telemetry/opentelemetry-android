package io.opentelemetry.android.instrumentation.slowrendering

import android.util.Log
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import java.time.Instant

private const val SLOW_THRESHOLD_MS = 16
private const val FROZEN_THRESHOLD_MS = 700

internal class SpanBasedJankReporter(private val tracer: Tracer) : JankReporter {

    override fun reportSlow(listener: PerActivityListener) {
        var slowCount = 0
        var frozenCount = 0
        val durationToCountHistogram = listener.resetMetrics()
        for (i in 0 until durationToCountHistogram.size()) {
            val duration = durationToCountHistogram.keyAt(i)
            val count = durationToCountHistogram.get(duration)
            if (duration > FROZEN_THRESHOLD_MS) {
                Log.d(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "* FROZEN RENDER DETECTED: $duration ms.$count times"
                )
                frozenCount += count
            } else if (duration > SLOW_THRESHOLD_MS) {
                Log.d(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "* Slow render detected: $duration ms. $count times"
                )
                slowCount += count
            }
        }

        val now = Instant.now();
        if (slowCount > 0) {
            makeSpan("slowRenders", listener.getActivityName(), slowCount, now);
        }
        if (frozenCount > 0) {
            makeSpan("frozenRenders", listener.getActivityName(), frozenCount, now);
        }
    }

    private fun makeSpan(spanName: String, activityName: String, slowCount: Int, now: Instant) {
        // TODO: Use an event rather than a zero-duration span
        val span: Span =
            tracer.spanBuilder(spanName)
                .setAttribute("count", slowCount.toLong())
                .setAttribute("activity.name", activityName)
                .setStartTimestamp(now)
                .startSpan()
        span.end(now)
    }
}