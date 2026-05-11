package io.opentelemetry.android.internal.services.periodic

import io.opentelemetry.android.internal.services.Service

interface TaskScheduler : Service {

    fun start(runnable: PeriodicRunnable)
}
