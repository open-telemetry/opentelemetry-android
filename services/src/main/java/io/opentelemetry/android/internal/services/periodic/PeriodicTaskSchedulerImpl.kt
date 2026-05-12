package io.opentelemetry.android.internal.services.periodic

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PeriodicTaskSchedulerImpl(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PeriodicTaskScheduler {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + dispatcher)

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
