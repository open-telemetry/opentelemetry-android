package io.opentelemetry.android.session

/**
 * The SessionManager is a public-facing tag interface that brings together
 * the SessionProvider and SessionPublisher interfaces under a common
 * name.
 */
interface SessionManager: SessionProvider, SessionPublisher {
}