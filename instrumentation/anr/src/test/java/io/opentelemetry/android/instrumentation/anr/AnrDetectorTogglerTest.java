/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnrDetectorTogglerTest {

    @Mock Runnable anrWatcher;
    @Mock ScheduledExecutorService scheduler;
    @Mock ScheduledFuture<?> future;

    @InjectMocks AnrDetectorToggler underTest;

    @Test
    void testOnApplicationForegrounded() {
        doReturn(future).when(scheduler).scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS);

        underTest.onApplicationForegrounded();
        underTest.onApplicationForegrounded();
        underTest.onApplicationForegrounded();

        verify(scheduler, times(1)).scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS);
    }

    @Test
    void testOnApplicationBackgrounded() {
        doReturn(future).when(scheduler).scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS);

        underTest.onApplicationForegrounded();

        underTest.onApplicationBackgrounded();
        underTest.onApplicationBackgrounded();
        underTest.onApplicationBackgrounded();

        verify(future, times(1)).cancel(true);
    }
}
