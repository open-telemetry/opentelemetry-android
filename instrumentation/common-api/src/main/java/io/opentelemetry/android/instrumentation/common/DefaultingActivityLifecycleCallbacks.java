/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common;

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
