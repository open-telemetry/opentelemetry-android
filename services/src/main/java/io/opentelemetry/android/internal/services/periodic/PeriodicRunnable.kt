package io.opentelemetry.android.internal.services.periodic

import kotlin.time.Duration

interface PeriodicRunnable : Runnable, Stoppable {

    fun period(): Duration

}
