/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

import android.os.Handler;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class AnrWatcher implements Runnable {
    private final AtomicInteger anrCounter = new AtomicInteger();
    private final Handler uiHandler;
    private final Thread mainThread;
    private final Instrumenter<StackTraceElement[], Void> instrumenter;

    AnrWatcher(
            Handler uiHandler,
            Thread mainThread,
            Instrumenter<StackTraceElement[], Void> instrumenter) {
        this.uiHandler = uiHandler;
        this.mainThread = mainThread;
        this.instrumenter = instrumenter;
    }

    @Override
    public void run() {
        CountDownLatch response = new CountDownLatch(1);
        if (!uiHandler.post(response::countDown)) {
            // the main thread is probably shutting down. ignore and return.
            return;
        }
        boolean success;
        try {
            success = response.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return;
        }
        if (success) {
            anrCounter.set(0);
            return;
        }
        if (anrCounter.incrementAndGet() >= 5) {
            StackTraceElement[] stackTrace = mainThread.getStackTrace();
            recordAnr(stackTrace);
            // only report once per 5s.
            anrCounter.set(0);
        }
    }

    private void recordAnr(StackTraceElement[] stackTrace) {
        Context context = instrumenter.start(Context.current(), stackTrace);
        instrumenter.end(context, stackTrace, null, null);
    }
}
