/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal;

import android.app.Activity;
import androidx.annotation.NonNull;
import io.opentelemetry.rum.internal.instrumentation.ApplicationStateListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class ApplicationStateWatcher implements DefaultingActivityLifecycleCallbacks {

    private final List<ApplicationStateListener> applicationStateListeners =
            new CopyOnWriteArrayList<>();
    // we count the number of activities that have been "started" and not yet "stopped" here to
    // figure out when the app goes into the background.
    private int numberOfOpenActivities = 0;

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (numberOfOpenActivities == 0) {
            for (ApplicationStateListener listener : applicationStateListeners) {
                listener.onApplicationForegrounded();
            }
        }
        numberOfOpenActivities++;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (--numberOfOpenActivities == 0) {
            for (ApplicationStateListener listener : applicationStateListeners) {
                listener.onApplicationBackgrounded();
            }
        }
    }

    void registerListener(ApplicationStateListener listener) {
        applicationStateListeners.add(listener);
    }
}
