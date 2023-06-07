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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BandwidthTrackerTest {

    private Clock clock;
    private AtomicLong time;

    @BeforeEach
    void setup() {
        clock = mock(Clock.class);
        time = new AtomicLong(System.currentTimeMillis());
        // Clock moves 5s each time its queried
        doAnswer(invocation -> time.addAndGet(5000)).when(clock).millis();
    }

    @Test
    void testSustainedRateNoData() {
        BandwidthTracker tracker = new BandwidthTracker(clock);
        assertEquals(0.0, tracker.totalSustainedRate(), 0.0);
    }

    @Test
    void testSustainedRate() {
        BandwidthTracker tracker = new BandwidthTracker(clock);
        tracker.tick(Collections.singletonList(new byte[270000]));
        tracker.tick(Collections.singletonList(new byte[200]));
        tracker.tick(Collections.singletonList(new byte[200]));
        tracker.tick(Collections.singletonList(new byte[200]));
        tracker.tick(Collections.singletonList(new byte[200]));
        tracker.tick(Collections.singletonList(new byte[200]));
        // 1000 bytes in the last 5 ticks, 25 seconds => 40bps
        double result = tracker.totalSustainedRate();
        assertEquals(40.0, result, 0.0);
    }
}
