/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.util.Log
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.semconv.events.AppJankEvent
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.kotlin.semconv.IncubatingApi

internal class EventJankReporter(
    private val eventLogger: Logger,
    private val threshold: Double,
    private val debugVerbose: Boolean = false,
) : JankReporter {
    @OptIn(IncubatingApi::class)
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
            AppJankEvent(
                appJankFrameCount = frameCount,
                appJankPeriod = periodSeconds,
                appJankThreshold = threshold,
            ).emit(
                logger = eventLogger,
            )
        }
    }
}
