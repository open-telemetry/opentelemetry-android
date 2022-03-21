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

package com.splunk.rum;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.common.Clock;

class SessionId {

    private static final long SESSION_LIFETIME_NANOS = TimeUnit.HOURS.toNanos(4);
    private static final long SESSION_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(15);
    private static final long NO_TIMEOUT = -1;

    private final Clock clock;
    private final AtomicReference<String> value = new AtomicReference<>();
    private final SessionIdTimeoutHandler timeoutHandler;
    private volatile long createTimeNanos;
    private volatile SessionIdChangeListener sessionIdChangeListener;

    SessionId(SessionIdTimeoutHandler timeoutHandler) {
        this(Clock.getDefault(), timeoutHandler);
    }

    //for testing
    SessionId(Clock clock, SessionIdTimeoutHandler timeoutHandler) {
        this.clock = clock;
        this.timeoutHandler = timeoutHandler;
        value.set(createNewId());
        createTimeNanos = clock.now();
    }

    private static String createNewId() {
        Random random = new Random();
        //The OTel TraceId has exactly the same format as a RUM SessionId, so let's re-use it here, rather
        //than re-inventing the wheel.
        return TraceId.fromLongs(random.nextLong(), random.nextLong());
    }

    String getSessionId() {
        String oldValue = value.get();
        String currentValue = oldValue;
        boolean sessionIdChanged = false;

        if (sessionExpired() || timeoutHandler.hasTimedOut()) {
            String newId = createNewId();
            //if this returns false, then another thread updated the value already.
            sessionIdChanged = value.compareAndSet(oldValue, newId);
            if (sessionIdChanged) {
                createTimeNanos = clock.now();
            }
            currentValue = value.get();
        }

        timeoutHandler.bump();
        // sessionId change listener needs tobe called after bumping the timer because it may create a new span
        if (sessionIdChanged && sessionIdChangeListener != null) {
            sessionIdChangeListener.onChange(oldValue, currentValue);
        }

        return currentValue;
    }

    private boolean sessionExpired() {
        // TODO: it probably should use nanoTime(); now() javadoc explicitly states that it's not meant to be used to compute duration
        long elapsedTime = clock.now() - createTimeNanos;
        return elapsedTime >= SESSION_LIFETIME_NANOS;
    }

    void setSessionIdChangeListener(SessionIdChangeListener sessionIdChangeListener) {
        this.sessionIdChangeListener = sessionIdChangeListener;
    }

    @Override
    public String toString() {
        return value.get();
    }
}
