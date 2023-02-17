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
