/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.startup;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
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
    @Nullable private AnchoredClock startupClock = null;
    private long firstPossibleTimestamp;
    @Nullable private volatile Span overallAppStartSpan = null;
    @Nullable private volatile Runnable completionCallback = null;
    // whether activity has been created
    // accessed only from UI thread
    private boolean uiInitStarted = false;
    // whether MAX_TIME_TO_UI_INIT has been exceeded
    // accessed only from UI thread
    private boolean uiInitTooLate = false;
    // accessed only from UI thread
    private final boolean isStartedFromBackground = false;

    public Span start(Tracer tracer, Clock clock) {
        // guard against a double-start and just return what's already in flight.
        if (overallAppStartSpan != null) {
            return overallAppStartSpan;
        }
        startupClock = AnchoredClock.create(clock);
        firstPossibleTimestamp = startupClock.now();
        final Span appStart =
                tracer.spanBuilder("AppStart")
                        .setStartTimestamp(firstPossibleTimestamp, TimeUnit.NANOSECONDS)
                        .setAttribute(RumConstants.START_TYPE_KEY, "cold")
                        .startSpan();
        overallAppStartSpan = appStart;
        return appStart;
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
        if (startupClock != null
                && firstPossibleTimestamp + MAX_TIME_TO_UI_INIT < startupClock.now()) {
            Log.d(RumConstants.OTEL_RUM_LOG_TAG, "Max time to UI init exceeded");
            uiInitTooLate = true;
            clear();
        }
    }

    public void end() {
        Span overallAppStartSpan = this.overallAppStartSpan;
        if (startupClock != null
                && overallAppStartSpan != null
                && !uiInitTooLate
                && !isStartedFromBackground) {
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
}
