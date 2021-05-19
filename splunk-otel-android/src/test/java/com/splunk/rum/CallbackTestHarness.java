package com.splunk.rum;

import android.app.Activity;
import android.os.Bundle;

import static org.mockito.Mockito.mock;

public class CallbackTestHarness {

    private final RumLifecycleCallbacks callbacks;

    public CallbackTestHarness(RumLifecycleCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void runAppStartupLifecycle(Activity activity) {
        Bundle bundle = mock(Bundle.class);

        callbacks.onActivityPreCreated(activity, bundle);
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityPostCreated(activity, bundle);

        callbacks.onActivityPreStarted(activity);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityPostStarted(activity);

        callbacks.onActivityPreResumed(activity);
        callbacks.onActivityResumed(activity);
        callbacks.onActivityPostResumed(activity);
    }
}
