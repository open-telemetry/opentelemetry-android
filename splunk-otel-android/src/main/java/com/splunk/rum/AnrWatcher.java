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

import android.os.Handler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

class AnrWatcher implements Runnable {
    private final AtomicInteger anrCounter = new AtomicInteger();
    private final Handler uiHandler;
    private final Thread mainThread;
    private final Supplier<SplunkRum> splunkRumSupplier;

    AnrWatcher(Handler uiHandler, Thread mainThread, Supplier<SplunkRum> splunkRumSupplier) {
        this.uiHandler = uiHandler;
        this.mainThread = mainThread;
        this.splunkRumSupplier = splunkRumSupplier;
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
            splunkRumSupplier.get().recordAnr(stackTrace);
            // only report once per 5s.
            anrCounter.set(0);
        }
    }
}
