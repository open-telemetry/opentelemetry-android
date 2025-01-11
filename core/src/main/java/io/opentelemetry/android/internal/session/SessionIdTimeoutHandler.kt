/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.session

import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener
import io.opentelemetry.sdk.common.Clock
import java.time.Duration

/**
 * This class encapsulates the following criteria about the sessionId timeout:
 *
 *
 *  * If the app is in the foreground sessionId should never time out.
 *  * If the app is in the background and no activity (spans) happens for >15 minutes, sessionId
 * should time out.
 *  * If the app is in the background and some activity (spans) happens in <15 minute intervals,
 * sessionId should not time out.
 *
 *
 * Consequently, when the app spent >15 minutes without any activity (spans) in the background,
 * after moving to the foreground the first span should trigger the sessionId timeout.
 */
internal class SessionIdTimeoutHandler(
    private val clock: Clock,
    private val sessionTimeout: Duration,
) : ApplicationStateListener {
    @Volatile
    private var timeoutStartNanos: Long = 0

    @Volatile
    private var state = State.FOREGROUND

    // for testing
    @JvmOverloads
    internal constructor(sessionTimeout: Duration = DEFAULT_SESSION_TIMEOUT) : this(
        Clock.getDefault(),
        sessionTimeout,
    )

    override fun onApplicationForegrounded() {
        state = State.TRANSITIONING_TO_FOREGROUND
    }

    override fun onApplicationBackgrounded() {
        state = State.BACKGROUND
    }

    fun hasTimedOut(): Boolean {
        // don't apply sessionId timeout to apps in the foreground
        if (state == State.FOREGROUND) {
            return false
        }
        val elapsedTime = clock.nanoTime() - timeoutStartNanos
        return elapsedTime >= sessionTimeout.toNanos()
    }

    fun bump() {
        timeoutStartNanos = clock.nanoTime()

        // move from the temporary transition state to foreground after the first span
        if (state == State.TRANSITIONING_TO_FOREGROUND) {
            state = State.FOREGROUND
        }
    }

    private enum class State {
        FOREGROUND,
        BACKGROUND,

        /** A temporary state representing the first event after the app has been brought back.  */
        TRANSITIONING_TO_FOREGROUND,
    }

    companion object {
        @JvmField
        val DEFAULT_SESSION_TIMEOUT: Duration = Duration.ofMinutes(15)
    }
}
