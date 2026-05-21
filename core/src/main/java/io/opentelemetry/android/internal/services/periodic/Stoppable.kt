package io.opentelemetry.android.internal.services.periodic

/**
 * A simple stop contract for scheduled work.
 */
internal interface Stoppable {

    fun stop()
    fun shouldStop(): Boolean
}
