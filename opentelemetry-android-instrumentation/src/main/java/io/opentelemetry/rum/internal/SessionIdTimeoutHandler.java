/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.rum.internal;

import io.opentelemetry.rum.internal.instrumentation.ApplicationStateListener;
import io.opentelemetry.sdk.common.Clock;
import java.util.concurrent.TimeUnit;

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

    private static final long SESSION_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(15);

    private final Clock clock;
    private volatile long timeoutStartNanos;
    private volatile State state = State.FOREGROUND;

    SessionIdTimeoutHandler() {
        this(Clock.getDefault());
    }

    // for testing
    SessionIdTimeoutHandler(Clock clock) {
        this.clock = clock;
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
        return elapsedTime >= SESSION_TIMEOUT_NANOS;
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
