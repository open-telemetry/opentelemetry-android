package io.opentelemetry.android.instrumentation.slowrendering

/**
 * Responsible for sending telemetry. This is a temporary class that we can remove
 * after the jank semconv becomes more stable.
 */
internal fun interface JankReporter {
    fun reportSlow(listener: PerActivityListener)

    /**
     * Creates a combined JankReporter that will first report slow for this
     * instance and then delegate to another JankReporter instance.
     */
    fun combine(jankReporter: JankReporter): JankReporter {
        return object: JankReporter {
            override fun reportSlow(listener: PerActivityListener) {
                reportSlow(listener)
                jankReporter.reportSlow(listener)
            }
        }
    }
}