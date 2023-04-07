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

package io.opentelemetry.rum.internal.instrumentation.anr;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class AnrDetectorTogglerTest {

    @Mock Runnable anrWatcher;
    @Mock ScheduledExecutorService scheduler;
    @Mock ScheduledFuture<?> future;

    @InjectMocks AnrDetectorToggler underTest;

    @Test
    void testOnApplicationForegrounded() {
        doReturn(future).when(scheduler).scheduleAtFixedRate(anrWatcher, 1, 1, TimeUnit.SECONDS);

        underTest.onApplicationForegrounded();
        underTest.onApplicationForegrounded();
        underTest.onApplicationForegrounded();

        verify(scheduler, times(1)).scheduleAtFixedRate(anrWatcher, 1, 1, TimeUnit.SECONDS);
    }

    @Test
    void testOnApplicationBackgrounded() {
        doReturn(future).when(scheduler).scheduleAtFixedRate(anrWatcher, 1, 1, TimeUnit.SECONDS);

        underTest.onApplicationForegrounded();

        underTest.onApplicationBackgrounded();
        underTest.onApplicationBackgrounded();
        underTest.onApplicationBackgrounded();

        verify(future, times(1)).cancel(true);
    }
}
