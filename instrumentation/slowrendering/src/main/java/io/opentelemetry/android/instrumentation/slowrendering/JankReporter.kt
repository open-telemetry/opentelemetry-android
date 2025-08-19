package io.opentelemetry.android.instrumentation.slowrendering

import android.util.SparseIntArray

/**
 * Responsible for sending telemetry. This is a temporary class that we can remove
 * after the jank semconv becomes more stable.
 */
internal fun interface JankReporter {
    fun reportSlow(durationToCountHistogram: SparseIntArray, periodSeconds: Double, activityName: String)

    /**
     * Creates a combined JankReporter that will first report slow for this
     * instance and then delegate to another JankReporter instance.
     */
    fun combine(jankReporter: JankReporter): JankReporter {
        if(jankReporter == this){
            throw IllegalArgumentException("cannot combine with self")
        }
        val exec = this::reportSlow
        return object: JankReporter {
            override fun reportSlow(durationToCountHistogram: SparseIntArray, periodSeconds: Double, activityName: String) {
                exec(durationToCountHistogram, periodSeconds, activityName)
                jankReporter.reportSlow(durationToCountHistogram, periodSeconds, activityName)
            }
        }
    }
}