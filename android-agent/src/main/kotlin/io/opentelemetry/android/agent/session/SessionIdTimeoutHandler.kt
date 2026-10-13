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
 * Tracks background and optional user inactivity independently of telemetry reads. Returning to
 * the foreground stops only the background timer and preserves expiry until the session rotates.
 * The configured clock must provide monotonic nanoseconds that include device sleep.
 */
internal class SessionIdTimeoutHandler(
    private val clock: Clock,
    private val sessionBackgroundInactivityTimeout: Duration,
    private val userInactivityTimeout: Duration? = null,
) : ApplicationStateListener {
    private val lock = Any()
    private var foreground = true
    private var timeoutStartNanos = 0L
    private var backgroundStartNanos = 0L
    private var expiredOnForeground = false

    // for testing
    @OptIn(Incubating::class)
    internal constructor(sessionConfig: SessionConfig, clock: Clock) : this(
        clock,
        sessionConfig.backgroundInactivityTimeout,
        sessionConfig.userInactivityTimeout,
    )

    override fun onApplicationForegrounded() {
        synchronized(lock) {
            expiredOnForeground = hasTimedOutLocked(clock.nanoTime())
            foreground = true
        }
    }

    override fun onApplicationBackgrounded() {
        synchronized(lock) {
            if (foreground) backgroundStartNanos = clock.nanoTime()
            foreground = false
        }
    }

    fun hasTimedOut(): Boolean = synchronized(lock) { hasTimedOutLocked(clock.nanoTime()) }

    private fun hasTimedOutLocked(now: Long): Boolean =
        expiredOnForeground ||
            (userInactivityTimeout?.let { now - timeoutStartNanos >= it.inWholeNanoseconds } ?: false) ||
            (!foreground && now - backgroundStartNanos >= sessionBackgroundInactivityTimeout.inWholeNanoseconds)

    fun bump() {
        synchronized(lock) {
            bumpLocked(clock.nanoTime())
        }
    }

    /** Refresh active input without allocating state or reviving an expired session. */
    fun bumpIfActive(): Boolean =
        synchronized(lock) {
            val now = clock.nanoTime()
            if (hasTimedOutLocked(now)) return false
            bumpLocked(now)
            true
        }

    private fun bumpLocked(now: Long) {
        timeoutStartNanos = now
        backgroundStartNanos = now
        expiredOnForeground = false
    }
}
