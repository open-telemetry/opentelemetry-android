/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

import androidx.annotation.Nullable;
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class AnrDetectorToggler implements ApplicationStateListener {

    private final Runnable anrWatcher;
    private final ScheduledExecutorService anrScheduler;

    @Nullable private ScheduledFuture<?> future;

    AnrDetectorToggler(Runnable anrWatcher, ScheduledExecutorService anrScheduler) {
        this.anrWatcher = anrWatcher;
        this.anrScheduler = anrScheduler;
    }

    @Override
    public void onApplicationForegrounded() {
        if (future == null) {
            future = anrScheduler.scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onApplicationBackgrounded() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }
}
