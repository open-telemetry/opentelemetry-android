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

    private final Clock clock;
    private final AtomicReference<String> value = new AtomicReference<>();
    private volatile long createTimeNanos;

    SessionId() {
        this(Clock.getDefault());
    }

    //for testing
    SessionId(Clock clock) {
        this.clock = clock;
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
        String currentValue = value.get();
        if (currentValue == null || sessionExpired()) {
            String newId = createNewId();
            //if this returns false, then another thread updated the value already.
            if (value.compareAndSet(currentValue, newId)) {
                createTimeNanos = clock.now();
            }
            return value.get();
        }
        return currentValue;
    }

    private boolean sessionExpired() {
        long elapsedTime = clock.now() - createTimeNanos;
        return elapsedTime >= SESSION_LIFETIME_NANOS;
    }

    @Override
    public String toString() {
        return value.get();
    }
}
