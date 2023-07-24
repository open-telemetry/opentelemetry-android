/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity;

import static org.mockito.Mockito.mock;

import android.app.Activity;
import android.os.Bundle;

class ActivityCallbackTestHarness {

    private final ActivityCallbacks callbacks;

    ActivityCallbackTestHarness(ActivityCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    void runAppStartupLifecycle(Activity mainActivity) {
        // app startup lifecycle is the same as a normal activity lifecycle
        runActivityCreationLifecycle(mainActivity);
    }

    void runActivityCreationLifecycle(Activity activity) {
        Bundle bundle = mock(Bundle.class);

        callbacks.onActivityPreCreated(activity, bundle);
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityPostCreated(activity, bundle);

        runActivityStartedLifecycle(activity);
        runActivityResumedLifecycle(activity);
    }

    void runActivityStartedLifecycle(Activity activity) {
        callbacks.onActivityPreStarted(activity);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityPostStarted(activity);
    }

    void runActivityPausedLifecycle(Activity activity) {
        callbacks.onActivityPrePaused(activity);
        callbacks.onActivityPaused(activity);
        callbacks.onActivityPostPaused(activity);
    }

    void runActivityResumedLifecycle(Activity activity) {
        callbacks.onActivityPreResumed(activity);
        callbacks.onActivityResumed(activity);
        callbacks.onActivityPostResumed(activity);
    }

    void runActivityStoppedFromRunningLifecycle(Activity activity) {
        runActivityPausedLifecycle(activity);
        runActivityStoppedFromPausedLifecycle(activity);
    }

    void runActivityStoppedFromPausedLifecycle(Activity activity) {
        callbacks.onActivityPreStopped(activity);
        callbacks.onActivityStopped(activity);
        callbacks.onActivityPostStopped(activity);
    }

    void runActivityDestroyedFromStoppedLifecycle(Activity activity) {
        callbacks.onActivityPreDestroyed(activity);
        callbacks.onActivityDestroyed(activity);
        callbacks.onActivityPostDestroyed(activity);
    }

    void runActivityDestroyedFromPausedLifecycle(Activity activity) {
        runActivityStoppedFromPausedLifecycle(activity);
        runActivityDestroyedFromStoppedLifecycle(activity);
    }

    void runActivityDestroyedFromRunningLifecycle(Activity activity) {
        runActivityStoppedFromRunningLifecycle(activity);
        runActivityDestroyedFromStoppedLifecycle(activity);
    }

    void runActivityRestartedLifecycle(Activity activity) {
        runActivityStartedLifecycle(activity);
        runActivityResumedLifecycle(activity);
    }
}
