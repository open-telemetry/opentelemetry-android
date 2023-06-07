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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.os.Handler;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnrWatcherTest {

    @RegisterExtension
    static final OpenTelemetryExtension testing = OpenTelemetryExtension.create();

    @Mock Handler handler;
    @Mock Thread mainThread;
    @Mock Instrumenter<StackTraceElement[], Void> instrumenter;

    @Test
    void mainThreadDisappearing() {
        AnrWatcher anrWatcher = new AnrWatcher(handler, mainThread, instrumenter);
        for (int i = 0; i < 5; i++) {
            when(handler.post(isA(Runnable.class))).thenReturn(false);
            anrWatcher.run();
        }
        verifyNoInteractions(instrumenter);
    }

    @Test
    void noAnr() {
        AnrWatcher anrWatcher = new AnrWatcher(handler, mainThread, instrumenter);
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
        verifyNoInteractions(instrumenter);
    }

    @Test
    void noAnr_temporaryPause() {
        AnrWatcher anrWatcher = new AnrWatcher(handler, mainThread, instrumenter);
        for (int i = 0; i < 5; i++) {
            int index = i;
            when(handler.post(isA(Runnable.class)))
                    .thenAnswer(
                            invocation -> {
                                Runnable callback = invocation.getArgument(0);
                                // have it fail once
                                if (index != 3) {
                                    callback.run();
                                }
                                return true;
                            });
            anrWatcher.run();
        }
        verifyNoInteractions(instrumenter);
    }

    @Test
    void anr_detected() {
        StackTraceElement[] stackTrace = new StackTraceElement[0];
        when(mainThread.getStackTrace()).thenReturn(stackTrace);

        AnrWatcher anrWatcher = new AnrWatcher(handler, mainThread, instrumenter);
        when(handler.post(isA(Runnable.class))).thenReturn(true);
        for (int i = 0; i < 5; i++) {
            anrWatcher.run();
        }
        verify(instrumenter, times(1)).start(any(), same(stackTrace));
        verify(instrumenter, times(1)).end(any(), same(stackTrace), isNull(), isNull());
        for (int i = 0; i < 4; i++) {
            anrWatcher.run();
        }
        verifyNoMoreInteractions(instrumenter);

        anrWatcher.run();
        verify(instrumenter, times(2)).start(any(), same(stackTrace));
        verify(instrumenter, times(2)).end(any(), same(stackTrace), isNull(), isNull());
    }
}
