package com.splunk.rum;

import android.app.Activity;
import android.os.Bundle;

import static org.mockito.Mockito.mock;

class Pre29ActivityCallbackTestHarness {

    private final Pre29ActivityCallbacks callbacks;

    Pre29ActivityCallbackTestHarness(Pre29ActivityCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    void runAppStartupLifecycle(Activity mainActivity) {
        //app startup lifecycle is the same as a normal activity lifecycle
        runActivityCreationLifecycle(mainActivity);
    }

    void runActivityCreationLifecycle(Activity activity) {
        Bundle bundle = mock(Bundle.class);

        callbacks.onActivityCreated(activity, bundle);

        runActivityStartedLifecycle(activity);
        runActivityResumedLifecycle(activity);
    }

    void runActivityStartedLifecycle(Activity activity) {
        callbacks.onActivityStarted(activity);
    }

    void runActivityPausedLifecycle(Activity activity) {
        callbacks.onActivityPaused(activity);
    }

    void runActivityResumedLifecycle(Activity activity) {
        callbacks.onActivityResumed(activity);
    }

    void runActivityStoppedFromRunningLifecycle(Activity activity) {
        runActivityPausedLifecycle(activity);
        runActivityStoppedFromPausedLifecycle(activity);
    }

    void runActivityStoppedFromPausedLifecycle(Activity activity) {
        callbacks.onActivityStopped(activity);
    }

    void runActivityDestroyedFromStoppedLifecycle(Activity activity) {
        callbacks.onActivityDestroyed(activity);
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
