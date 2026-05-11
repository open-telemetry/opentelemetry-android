package io.opentelemetry.android.internal.services.schedule

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WorkSchedulerImpl : WorkScheduler {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)

    override fun start(runnable: PeriodicRunnable) {
        scope.launch {
            while (!runnable.shouldStop()) {
                runnable.run()
                delay(runnable.period())
            }
        }
    }

    override fun close() {
        job.cancel()
    }
}
