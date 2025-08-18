package io.opentelemetry.android.instrumentation.slowrendering

/**
 * Responsible for sending telemetry. This is a temporary class that we can remove
 * after the jank semconv becomes more stable.
 */
internal interface JankReporter {
    fun reportSlow(listener: PerActivityListener)
}