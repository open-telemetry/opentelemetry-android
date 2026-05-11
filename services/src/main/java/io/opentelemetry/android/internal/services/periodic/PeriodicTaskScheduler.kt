package io.opentelemetry.android.internal.services.periodic

import io.opentelemetry.android.internal.services.Service

/**
 * Schedules periodic tasks to run in the background.
 * Implementations are responsible for starting and stopping their own execution resources.
 */
interface PeriodicTaskScheduler : Service {

    /**
     * Starts executing the supplied periodic task.
     * Implementations should repeatedly execute this runnable on its period until it is stopped.
     */
    fun start(runnable: PeriodicRunnable)
}
