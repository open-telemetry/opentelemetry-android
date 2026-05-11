package io.opentelemetry.android.internal.services.schedule

import kotlin.time.Duration

interface PeriodicRunnable : Runnable, Stoppable {

    fun period(): Duration

}
