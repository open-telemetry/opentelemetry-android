/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

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
class OldAnrWatcherTest {

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
