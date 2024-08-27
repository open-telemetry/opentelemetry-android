package io.opentelemetry.android.session

interface SessionProvider {

    fun getSessionId(): String
}