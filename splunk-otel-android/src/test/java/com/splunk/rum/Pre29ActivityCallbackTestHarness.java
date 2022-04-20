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

import static org.mockito.Mockito.mock;

import android.app.Activity;
import android.os.Bundle;

class Pre29ActivityCallbackTestHarness {

    private final Pre29ActivityCallbacks callbacks;

    Pre29ActivityCallbackTestHarness(Pre29ActivityCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    void runAppStartupLifecycle(Activity mainActivity) {
        // app startup lifecycle is the same as a normal activity lifecycle
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
