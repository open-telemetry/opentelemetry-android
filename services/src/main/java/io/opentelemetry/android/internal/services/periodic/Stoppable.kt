package io.opentelemetry.android.internal.services.periodic

/**
 * A simple stop contract for scheduled work.
 */
interface Stoppable {

    fun stop()
    fun shouldStop(): Boolean
}
