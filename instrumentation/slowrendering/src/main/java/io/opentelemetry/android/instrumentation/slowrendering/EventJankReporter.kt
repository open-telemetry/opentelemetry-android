/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.util.Log
import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.ktx.setSessionIdentifiersWith
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger

// TODO: Replace with semconv constants
internal val FRAME_COUNT: AttributeKey<Long> = AttributeKey.longKey("app.jank.frame_count")
internal val PERIOD: AttributeKey<Double> = AttributeKey.doubleKey("app.jank.period")
internal val THRESHOLD: AttributeKey<Double> = AttributeKey.doubleKey("app.jank.threshold")

@OptIn(Incubating::class)
internal class EventJankReporter(
    private val eventLogger: Logger,
    private val sessionProvider: SessionProvider,
    private val threshold: Double,
    private val debugVerbose: Boolean = false,
) : JankReporter {
    override fun reportSlow(
        durationToCountHistogram: Map<Int, Int>,
        periodSeconds: Double,
        activityName: String,
    ) {
        var frameCount: Long = 0
        for (entry in durationToCountHistogram) {
            val durationMillis = entry.key
            if ((durationMillis / 1000.0) > threshold) {
                val count = entry.value
                if (debugVerbose) {
                    Log.d(
                        RumConstants.OTEL_RUM_LOG_TAG,
                        "* Slow render detected: $durationMillis ms. $count times",
                    )
                }
                frameCount += count
            }
        }

        if (frameCount > 0) {
            val attributes =
                Attributes
                    .builder()
                    .put(FRAME_COUNT, frameCount)
                    .put(PERIOD, periodSeconds)
                    .put(THRESHOLD, threshold)
                    .build()
            eventLogger
                .logRecordBuilder()
                .setSessionIdentifiersWith(sessionProvider)
                .setEventName("app.jank")
                .setAllAttributes(attributes)
                .emit()
        }
    }
}
