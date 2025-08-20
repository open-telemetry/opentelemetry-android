/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.util.Log
import android.util.SparseIntArray
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder
import io.opentelemetry.api.logs.Logger

// TODO: Replace with semconv constants
val FRAME_COUNT: AttributeKey<Long> = AttributeKey.longKey("app.jank.frame_count")
val PERIOD: AttributeKey<Double> = AttributeKey.doubleKey("app.jank.period")
val THRESHOLD: AttributeKey<Double> = AttributeKey.doubleKey("app.jank.threshold")

internal class EventsJankReporter(
    private val eventLogger: Logger,
    private val threshold: Double,
) : JankReporter {
    override fun reportSlow(
        durationToCountHistogram: SparseIntArray,
        periodSeconds: Double,
        activityName: String,
    ) {
        var frameCount: Long = 0
        for (i in 0 until durationToCountHistogram.size()) {
            val durationMillis = durationToCountHistogram.keyAt(i)
            if ((durationMillis / 1000.0) > threshold) {
                val count = durationToCountHistogram.get(durationMillis)
                Log.d(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "* Slow render detected: $durationMillis ms. $count times",
                )
                frameCount += count
            }
        }

        if (frameCount > 0) {
            val eventBuilder = eventLogger.logRecordBuilder() as ExtendedLogRecordBuilder
            val attributes =
                Attributes
                    .builder()
                    .put(FRAME_COUNT, frameCount)
                    .put(PERIOD, periodSeconds)
                    .put(THRESHOLD, threshold)
                    .build()
            eventBuilder
                .setEventName("app.jank")
                .setAllAttributes(attributes)
                .emit()
        }
    }
}
