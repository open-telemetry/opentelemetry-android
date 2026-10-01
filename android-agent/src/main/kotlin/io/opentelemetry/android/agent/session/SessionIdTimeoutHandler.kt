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
 * the timeout; a new session or an explicit call to the internal user interaction recorder restarts it.
 * By default returning to the foreground stops the timer, but preserves an expiry until the
 * manager rotates the session. Opt-in interaction tracking also times out in the foreground.
 * The configured clock must provide monotonic nanoseconds that include device sleep.
 */
internal class SessionIdTimeoutHandler(
    private val clock: Clock,
    private val sessionBackgroundInactivityTimeout: Duration,
    private val userInactivityTimeout: Duration? = null,
) : ApplicationStateListener {
    private val lock = Any()

    @Volatile
    private var state = TimeoutState()

    // for testing
    @OptIn(Incubating::class)
    internal constructor(sessionConfig: SessionConfig, clock: Clock) : this(
        clock,
        sessionConfig.backgroundInactivityTimeout,
        sessionConfig.userInactivityTimeout,
    )

    override fun onApplicationForegrounded() {
        synchronized(lock) {
            state = state.copy(foreground = true, expiredOnForeground = hasTimedOut())
        }
    }

    override fun onApplicationBackgrounded() {
        synchronized(lock) {
            state =
                state.copy(
                    foreground = false,
                    backgroundStartNanos = if (state.foreground) clock.nanoTime() else state.backgroundStartNanos,
                )
        }
    }

    fun hasTimedOut(): Boolean {
        val current = state
        if (current.expiredOnForeground) {
            return true
        }
        val now = clock.nanoTime()
        val userExpired = userInactivityTimeout?.let { now - current.timeoutStartNanos >= it.inWholeNanoseconds } ?: false
        val backgroundExpired =
            !current.foreground && now - current.backgroundStartNanos >= sessionBackgroundInactivityTimeout.inWholeNanoseconds
        return userExpired || backgroundExpired
    }

    fun bump() {
        synchronized(lock) {
            val now = clock.nanoTime()
            state = state.copy(timeoutStartNanos = now, backgroundStartNanos = now, expiredOnForeground = false)
        }
    }

    private data class TimeoutState(
        val foreground: Boolean = true,
        val timeoutStartNanos: Long = 0,
        val backgroundStartNanos: Long = 0,
        val expiredOnForeground: Boolean = false,
    )
}
