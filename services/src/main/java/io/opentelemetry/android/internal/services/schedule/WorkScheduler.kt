package io.opentelemetry.android.internal.services.schedule

import io.opentelemetry.android.internal.services.Service

interface WorkScheduler : Service {

    fun start(runnable: PeriodicRunnable)

}
