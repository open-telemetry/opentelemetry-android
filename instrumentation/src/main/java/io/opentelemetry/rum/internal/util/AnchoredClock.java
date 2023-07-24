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

package io.opentelemetry.rum.internal.util;

import io.opentelemetry.sdk.common.Clock;

// copied from otel-java
public final class AnchoredClock {
    private final Clock clock;
    private final long epochNanos;
    private final long nanoTime;

    private AnchoredClock(Clock clock, long epochNanos, long nanoTime) {
        this.clock = clock;
        this.epochNanos = epochNanos;
        this.nanoTime = nanoTime;
    }

    public static AnchoredClock create(Clock clock) {
        return new AnchoredClock(clock, clock.now(), clock.nanoTime());
    }

    public long now() {
        long deltaNanos = this.clock.nanoTime() - this.nanoTime;
        return this.epochNanos + deltaNanos;
    }
}
