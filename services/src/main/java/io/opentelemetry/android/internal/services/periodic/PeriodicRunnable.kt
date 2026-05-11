package io.opentelemetry.android.internal.services.periodic

import kotlin.time.Duration

/**
 * A runnable task that is executed repeatedly on a fixed interval.
 * Implementations can signal when scheduling should stop.
 */
interface PeriodicRunnable : Runnable, Stoppable {

    /**
     * The interval between runs.
     */
    fun period(): Duration

}
