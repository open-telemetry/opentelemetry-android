/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common;

import android.app.Application;
import io.opentelemetry.sdk.OpenTelemetrySdk;

/**
 * Provides access to the {@linkplain OpenTelemetrySdk OpenTelemetry SDK}, the instrumented {@link
 * Application}, allows registering {@linkplain ApplicationStateListener listeners}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface InstrumentedApplication {

    /** Returns the instrumented {@link Application}. */
    Application getApplication();

    /** Returns the {@link OpenTelemetrySdk} instance. */
    OpenTelemetrySdk getOpenTelemetrySdk();

    /**
     * Registers the passed {@link ApplicationStateListener} - from now on it will be called
     * whenever the application is moved from background to foreground, and vice versa.
     *
     * <p>Users of this method should take care to avoid passing the same listener instance multiple
     * times; duplicates are not trimmed.
     */
    void registerApplicationStateListener(ApplicationStateListener listener);
}
