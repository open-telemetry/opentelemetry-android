/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating
import io.opentelemetry.sdk.common.Clock
import kotlin.time.Duration

/**
 * This class encapsulates the following criteria about the sessionId timeout:
 *
 *
 *  * While the user is active the sessionId should never time out.
 *  * If the user is inactive and no activity (spans) happens for >15 minutes, sessionId
 * should time out.
 *  * If the user is inactive and some activity (spans) happens in <15 minute intervals,
 * sessionId should not time out.
 *
 *
 * Consequently, when >15 minutes went by without any activity (spans) while the user was inactive,
 * the first span after the user becomes active again should trigger the sessionId timeout.
 *
 * Whether the user is active or not is reported through [onUserActive] and [onUserInactive].
 * Those are driven by [SessionActivityApi], which apps can call directly and which the agent's
 * default lifecycle based inference also writes through.
 */
internal class SessionIdTimeoutHandler(
    private val clock: Clock,
    private val sessionBackgroundInactivityTimeout: Duration,
) {
    @Volatile
    private var timeoutStartNanos: Long = 0

    @Volatile
    private var state = State.ACTIVE

    // for testing
    @OptIn(Incubating::class)
    internal constructor(sessionConfig: SessionConfig, clock: Clock) : this(
        clock,
        sessionConfig.backgroundInactivityTimeout,
    )

    fun onUserActive() {
        // Only leave the inactive state. The timeout is evaluated lazily on the next session id
        // request, so the first telemetry item after the user becomes active can still start a
        // new session when the inactivity timeout has elapsed.
        if (state == State.INACTIVE) {
            state = State.TRANSITIONING_TO_ACTIVE
        }
    }

    fun onUserInactive() {
        state = State.INACTIVE
    }

    fun hasTimedOut(): Boolean {
        // don't apply sessionId timeout while the user is active
        if (state == State.ACTIVE) {
            return false
        }
        val elapsedTime = clock.nanoTime() - timeoutStartNanos
        return elapsedTime >= sessionBackgroundInactivityTimeout.inWholeNanoseconds
    }

    fun bump() {
        timeoutStartNanos = clock.nanoTime()

        // move from the temporary transition state to active after the first span
        if (state == State.TRANSITIONING_TO_ACTIVE) {
            state = State.ACTIVE
        }
    }

    private enum class State {
        ACTIVE,
        INACTIVE,

        /** A temporary state representing the first event after the user became active again. */
        TRANSITIONING_TO_ACTIVE,
    }
}
