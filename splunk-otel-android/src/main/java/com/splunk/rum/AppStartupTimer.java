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
import android.util.Log;
import androidx.annotation.Nullable;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.common.Clock;
import java.util.concurrent.TimeUnit;

class AppStartupTimer {
    // Maximum time from app start to creation of the UI. If this time is exceeded we will not
    // create the app start span. Long app startup could indicate that the app was really started in
    // background, in which case the measured startup time is misleading.
    private static final long MAX_TIME_TO_UI_INIT = TimeUnit.MINUTES.toNanos(1);

    // exposed so it can be used for the rest of the startup sequence timing.
    final RumInitializer.AnchoredClock startupClock =
            RumInitializer.AnchoredClock.create(Clock.getDefault());
    private final long firstPossibleTimestamp = startupClock.now();
    private volatile Span overallAppStartSpan = null;
    private volatile Runnable completionCallback = null;
    // whether activity has been created
    // accessed only from UI thread
    private boolean uiInitStarted = false;
    // whether MAX_TIME_TO_UI_INIT has been exceeded
    // accessed only from UI thread
    private boolean uiInitTooLate = false;
    // accessed only from UI thread
    private boolean isStartedFromBackground = false;

    Span start(Tracer tracer) {
        // guard against a double-start and just return what's already in flight.
        if (overallAppStartSpan != null) {
            return overallAppStartSpan;
        }
        final Span appStart =
                tracer.spanBuilder("AppStart")
                        .setStartTimestamp(firstPossibleTimestamp, TimeUnit.NANOSECONDS)
                        .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_APPSTART)
                        .setAttribute(SplunkRum.START_TYPE_KEY, "cold")
                        .startSpan();
        overallAppStartSpan = appStart;
        return appStart;
    }

    /** Called when Activity is created. */
    void startUiInit() {
        if (uiInitStarted || isStartedFromBackground) {
            return;
        }
        uiInitStarted = true;
        if (firstPossibleTimestamp + MAX_TIME_TO_UI_INIT < startupClock.now()) {
            Log.d(SplunkRum.LOG_TAG, "Max time to UI init exceeded");
            uiInitTooLate = true;
            clear();
        }
    }

    void setCompletionCallback(Runnable completionCallback) {
        this.completionCallback = completionCallback;
    }

    void end() {
        if (overallAppStartSpan != null && !uiInitTooLate && !isStartedFromBackground) {
            runCompletionCallback();
            overallAppStartSpan.end(startupClock.now(), TimeUnit.NANOSECONDS);
        }
        clear();
    }

    @Nullable
    Span getStartupSpan() {
        return overallAppStartSpan;
    }

    // visibleForTesting
    void runCompletionCallback() {
        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    private void clear() {
        overallAppStartSpan = null;
        completionCallback = null;
    }

    void detectBackgroundStart(Handler handler) {
        handler.post(new StartFromBackgroundRunnable(this));
    }

    /**
     * See
     * https://github.com/firebase/firebase-android-sdk/blob/939f90edd74373d42772518d04826657c2ef2e21/firebase-perf/src/main/java/com/google/firebase/perf/metrics/AppStartTrace.java#L283
     * When a runnable posted to main UI thread is executed before any activity's onCreate() method
     * then the app is started in background. If app is started from foreground, activity's
     * onCreate() method is executed before this runnable. Firebase does this check from a
     * ContentProvider, we do it from whatever used SplunkRum first. If the first use of SplunkRum
     * happens when the app is already started for us it will look the same as a background start,
     * which is fine as it wouldn't report correct time anyway.
     */
    private static class StartFromBackgroundRunnable implements Runnable {
        private final AppStartupTimer startupTimer;

        public StartFromBackgroundRunnable(AppStartupTimer startupTimer) {
            this.startupTimer = startupTimer;
        }

        @Override
        public void run() {
            // check whether an activity has been created
            if (!startupTimer.uiInitStarted) {
                Log.d(SplunkRum.LOG_TAG, "Detected background app start");
                startupTimer.isStartedFromBackground = true;
            }
        }
    }
}
