package com.splunk.rum;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

public class BandwidthTrackerTest {

    private Clock clock;
    private AtomicLong time;

    @Before
    public void setup(){
        clock = mock(Clock.class);
        time = new AtomicLong(System.currentTimeMillis());
        // Clock moves 5s each time its queried
        doAnswer(invocation -> time.addAndGet(5000)).when(clock).millis();
    }

    @Test
    public void testSustainedRateNoData() {
        BandwidthTracker tracker = new BandwidthTracker(clock);
        assertEquals(0.0, tracker.totalSustainedRate(), 0.0);
    }

    @Test
    public void testSustainedRate() {
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