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
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface helper for implementations that don't need/want all the extra baggage of the full
 * Application.ActivityLifecycleCallbacks interface. Implementations can choose which methods to
 * implement.
 */
public interface DefaultingActivityLifecycleCallbacks
        extends Application.ActivityLifecycleCallbacks {

    @Override
    default void onActivityCreated(
            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    default void onActivityStarted(@NonNull Activity activity) {}

    @Override
    default void onActivityResumed(@NonNull Activity activity) {}

    @Override
    default void onActivityPaused(@NonNull Activity activity) {}

    @Override
    default void onActivityStopped(@NonNull Activity activity) {}

    @Override
    default void onActivitySaveInstanceState(
            @NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    default void onActivityDestroyed(@NonNull Activity activity) {}
}
