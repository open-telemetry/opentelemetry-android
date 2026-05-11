package io.opentelemetry.android.internal.services.schedule

interface Stoppable {

    fun stop()
    fun shouldStop(): Boolean
}
