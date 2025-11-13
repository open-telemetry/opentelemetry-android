/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.startup;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.common.Clock;
import java.util.concurrent.TimeUnit;

public class AppStartupTimer {
    // Maximum time from app start to creation of the UI. If this time is exceeded we will not
    // create the app start span. Long app startup could indicate that the app was really started in
    // background, in which case the measured startup time is misleading.
    private static final long MAX_TIME_TO_UI_INIT = TimeUnit.MINUTES.toNanos(1);

    // exposed so it can be used for the rest of the startup sequence timing.
    private final AnchoredClock startupClock = AnchoredClock.create(Clock.getDefault());
    private final long firstPossibleTimestamp = startupClock.now();
    @Nullable private volatile Span overallAppStartSpan = null;
    @Nullable private volatile Runnable completionCallback = null;
    // whether activity has been created
    // accessed only from UI thread
    private boolean uiInitStarted = false;
    // whether MAX_TIME_TO_UI_INIT has been exceeded
    // accessed only from UI thread
    private boolean uiInitTooLate = false;
    // accessed only from UI thread
    private boolean isStartedFromBackground = false;

    public Span start(Tracer tracer) {
        // guard against a double-start and just return what's already in flight.
        if (overallAppStartSpan != null) {
            return overallAppStartSpan;
        }
        final Span appStart =
                tracer.spanBuilder("AppStart")
                        .setStartTimestamp(firstPossibleTimestamp, TimeUnit.NANOSECONDS)
                        .setAttribute(RumConstants.START_TYPE_KEY, "cold")
                        .startSpan();
        overallAppStartSpan = appStart;
        return appStart;
    }

    /** Returns the epoch timestamp in nanos calculated by the startupClock. */
    public long clockNow() {
        return startupClock.now();
    }

    /**
     * Creates a lifecycle listener that starts the UI init when an activity is created.
     *
     * @return a new Application.ActivityLifecycleCallbacks instance
     */
    public Application.ActivityLifecycleCallbacks createLifecycleCallback() {
        return new DefaultingActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(
                    @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                startUiInit();
            }
        };
    }

    /** Called when Activity is created. */
    private void startUiInit() {
        if (uiInitStarted || isStartedFromBackground) {
            return;
        }
        uiInitStarted = true;
        if (firstPossibleTimestamp + MAX_TIME_TO_UI_INIT < startupClock.now()) {
            Log.d(RumConstants.OTEL_RUM_LOG_TAG, "Max time to UI init exceeded");
            uiInitTooLate = true;
            clear();
        }
    }

    public void setCompletionCallback(Runnable completionCallback) {
        this.completionCallback = completionCallback;
    }

    public void end() {
        Span overallAppStartSpan = this.overallAppStartSpan;
        if (overallAppStartSpan != null && !uiInitTooLate && !isStartedFromBackground) {
            runCompletionCallback();
            overallAppStartSpan.end(startupClock.now(), TimeUnit.NANOSECONDS);
        }
        clear();
    }

    @Nullable
    public Span getStartupSpan() {
        return overallAppStartSpan;
    }

    // visibleForTesting
    public void runCompletionCallback() {
        Runnable completionCallback = this.completionCallback;
        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    private void clear() {
        overallAppStartSpan = null;
        completionCallback = null;
    }

    public void detectBackgroundStart(Handler handler) {
        handler.post(new StartFromBackgroundRunnable(this));
    }

    /**
     * See
     * https://github.com/firebase/firebase-android-sdk/blob/939f90edd74373d42772518d04826657c2ef2e21/firebase-perf/src/main/java/com/google/firebase/perf/metrics/AppStartTrace.java#L283
     * When a runnable posted to main UI thread is executed before any activity's onCreate() method
     * then the app is started in background. If app is started from foreground, activity's
     * onCreate() method is executed before this runnable. Firebase does this check from a
     * ContentProvider, we do it from whatever used OpenTelemetryRum first. If the first use of
     * OpenTelemetryRum happens when the app is already started for us it will look the same as a
     * background start, which is fine as it wouldn't report correct time anyway.
     */
    private static class StartFromBackgroundRunnable implements Runnable {
        private final AppStartupTimer startupTimer;

        private StartFromBackgroundRunnable(AppStartupTimer startupTimer) {
            this.startupTimer = startupTimer;
        }

        @Override
        public void run() {
            // check whether an activity has been created
            if (!startupTimer.uiInitStarted) {
                Log.d(RumConstants.OTEL_RUM_LOG_TAG, "Detected background app start");
                startupTimer.isStartedFromBackground = true;
            }
        }
    }
}
