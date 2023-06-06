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

import androidx.annotation.Nullable;

import io.opentelemetry.rum.internal.instrumentation.ApplicationStateListener;

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
            future = anrScheduler.scheduleAtFixedRate(anrWatcher, 1, 1, TimeUnit.SECONDS);
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
