package io.opentelemetry.android.session

interface SessionPublisher {

    fun addObserver(observer: SessionObserver)
}