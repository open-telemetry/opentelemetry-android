/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener;
import io.opentelemetry.sdk.common.Clock;
import java.time.Duration;

/**
 * This class encapsulates the following criteria about the sessionId timeout:
 *
 * <ul>
 *   <li>If the app is in the foreground sessionId should never time out.
 *   <li>If the app is in the background and no activity (spans) happens for >15 minutes, sessionId
 *       should time out.
 *   <li>If the app is in the background and some activity (spans) happens in <15 minute intervals,
 *       sessionId should not time out.
 * </ul>
 *
 * <p>Consequently, when the app spent >15 minutes without any activity (spans) in the background,
 * after moving to the foreground the first span should trigger the sessionId timeout.
 */
final class SessionIdTimeoutHandler implements ApplicationStateListener {

    static final Duration DEFAULT_SESSION_TIMEOUT = Duration.ofMinutes(15);
    private final Duration sessionTimeout;

    private final Clock clock;
    private volatile long timeoutStartNanos;
    private volatile State state = State.FOREGROUND;

    SessionIdTimeoutHandler() {
        this(DEFAULT_SESSION_TIMEOUT);
    }

    // for testing
    SessionIdTimeoutHandler(Duration sessionTimeout) {
        this(Clock.getDefault(), sessionTimeout);
    }

    SessionIdTimeoutHandler(Clock clock, Duration sessionTimeout) {
        this.clock = clock;
        this.sessionTimeout = sessionTimeout;
    }

    @Override
    public void onApplicationForegrounded() {
        state = State.TRANSITIONING_TO_FOREGROUND;
    }

    @Override
    public void onApplicationBackgrounded() {
        state = State.BACKGROUND;
    }

    boolean hasTimedOut() {
        // don't apply sessionId timeout to apps in the foreground
        if (state == State.FOREGROUND) {
            return false;
        }
        long elapsedTime = clock.nanoTime() - timeoutStartNanos;
        return elapsedTime >= sessionTimeout.toNanos();
    }

    void bump() {
        timeoutStartNanos = clock.nanoTime();

        // move from the temporary transition state to foreground after the first span
        if (state == State.TRANSITIONING_TO_FOREGROUND) {
            state = State.FOREGROUND;
        }
    }

    private enum State {
        FOREGROUND,
        BACKGROUND,
        /** A temporary state representing the first event after the app has been brought back. */
        TRANSITIONING_TO_FOREGROUND
    }
}
