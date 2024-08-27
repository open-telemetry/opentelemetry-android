package io.opentelemetry.android.session

interface SessionObserver {

    fun onSessionStarted(session: Session) // TODO: Prior session included here?
    fun onSessionEnded(session: Session)
}