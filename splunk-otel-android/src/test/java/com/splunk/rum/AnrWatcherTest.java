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

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.os.Handler;
import org.junit.jupiter.api.Test;

class AnrWatcherTest {

    @Test
    void mainThreadDisappearing() {
        Handler handler = mock(Handler.class);
        Thread mainThread = mock(Thread.class);
        SplunkRum splunkRum = mock(SplunkRum.class);

        AnrWatcher anrWatcher = new AnrWatcher(handler, mainThread, () -> splunkRum);
        for (int i = 0; i < 5; i++) {
            when(handler.post(isA(Runnable.class))).thenReturn(false);
            anrWatcher.run();
        }
        verifyNoInteractions(splunkRum);
    }

    @Test
    void noAnr() {
        Handler handler = mock(Handler.class);
        Thread mainThread = mock(Thread.class);
        SplunkRum splunkRum = mock(SplunkRum.class);

        AnrWatcher anrWatcher = new AnrWatcher(handler, mainThread, () -> splunkRum);
        for (int i = 0; i < 5; i++) {
            when(handler.post(isA(Runnable.class)))
                    .thenAnswer(
                            invocation -> {
                                Runnable callback = (Runnable) invocation.getArgument(0);
                                callback.run();
                                return true;
                            });
            anrWatcher.run();
        }
        verifyNoInteractions(splunkRum);
    }

    @Test
    void noAnr_temporaryPause() {
        Handler handler = mock(Handler.class);
        Thread mainThread = mock(Thread.class);
        SplunkRum splunkRum = mock(SplunkRum.class);

        AnrWatcher anrWatcher = new AnrWatcher(handler, mainThread, () -> splunkRum);
        for (int i = 0; i < 5; i++) {
            int index = i;
            when(handler.post(isA(Runnable.class)))
                    .thenAnswer(
                            invocation -> {
                                Runnable callback = (Runnable) invocation.getArgument(0);
                                // have it fail once
                                if (index != 3) {
                                    callback.run();
                                }
                                return true;
                            });
            anrWatcher.run();
        }
        verifyNoInteractions(splunkRum);
    }

    @Test
    void anr_detected() {
        Handler handler = mock(Handler.class);
        Thread mainThread = mock(Thread.class);
        SplunkRum splunkRum = mock(SplunkRum.class);

        StackTraceElement[] stackTrace = new StackTraceElement[0];
        when(mainThread.getStackTrace()).thenReturn(stackTrace);

        AnrWatcher anrWatcher = new AnrWatcher(handler, mainThread, () -> splunkRum);
        when(handler.post(isA(Runnable.class))).thenReturn(true);
        for (int i = 0; i < 5; i++) {
            anrWatcher.run();
        }
        verify(splunkRum, times(1)).recordAnr(stackTrace);
        for (int i = 0; i < 4; i++) {
            anrWatcher.run();
        }
        verifyNoMoreInteractions(splunkRum);

        anrWatcher.run();
        verify(splunkRum, times(2)).recordAnr(stackTrace);
    }
}
