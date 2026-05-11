package io.opentelemetry.android.internal.services.periodic

interface Stoppable {

    fun stop()
    fun shouldStop(): Boolean
}
