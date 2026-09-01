/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating
import io.opentelemetry.sdk.common.Clock
import kotlin.time.Duration

/**
 * Tracks the elapsed time since the last meaningful session activity.
 */
internal class SessionIdTimeoutHandler(
    private val clock: Clock,
    private val sessionInactivityTimeout: Duration,
) {
    @Volatile
    private var timeoutStartNanos: Long = clock.nanoTime()

    // for testing
    @OptIn(Incubating::class)
    internal constructor(sessionConfig: SessionConfig, clock: Clock) : this(
        clock,
        sessionConfig.inactivityTimeout,
    )

    fun hasTimedOut(): Boolean {
        val elapsedTime = clock.nanoTime() - timeoutStartNanos
        return elapsedTime >= sessionInactivityTimeout.inWholeNanoseconds
    }

    fun bump() {
        timeoutStartNanos = clock.nanoTime()
    }
}
