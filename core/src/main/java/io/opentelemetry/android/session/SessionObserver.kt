package io.opentelemetry.android.session

interface SessionObserver {

    fun onSessionStarted(newSession: Session, previousSession: Session)
    fun onSessionEnded(session: Session)
}