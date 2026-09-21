/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener
import io.opentelemetry.sdk.common.Clock
import kotlin.time.Duration

/**
 * Tracks background inactivity independently of telemetry reads. Entering the background starts
 * the timeout; a new session or an explicit call to the internal activity recorder restarts it.
 * Activity sources are not wired to that recorder yet. Returning to the foreground stops the
 * timer, but preserves an expiry until the manager rotates the session.
 */
internal class SessionIdTimeoutHandler(
    private val clock: Clock,
    private val sessionBackgroundInactivityTimeout: Duration,
) : ApplicationStateListener {
    @Volatile
    private var state = TimeoutState()

    // for testing
    @OptIn(Incubating::class)
    internal constructor(sessionConfig: SessionConfig, clock: Clock) : this(
        clock,
        sessionConfig.backgroundInactivityTimeout,
    )

    @Synchronized
    override fun onApplicationForegrounded() {
        state = state.copy(foreground = true, expiredOnForeground = hasTimedOut())
    }

    @Synchronized
    override fun onApplicationBackgrounded() {
        state =
            state.copy(
                foreground = false,
                timeoutStartNanos = if (state.foreground) clock.nanoTime() else state.timeoutStartNanos,
            )
    }

    fun hasTimedOut(): Boolean {
        val current = state
        if (current.expiredOnForeground) {
            return true
        }
        return !current.foreground &&
            clock.nanoTime() - current.timeoutStartNanos >= sessionBackgroundInactivityTimeout.inWholeNanoseconds
    }

    @Synchronized
    fun bump() {
        state = state.copy(timeoutStartNanos = clock.nanoTime(), expiredOnForeground = false)
    }

    private data class TimeoutState(
        val foreground: Boolean = true,
        val timeoutStartNanos: Long = 0,
        val expiredOnForeground: Boolean = false,
    )
}
