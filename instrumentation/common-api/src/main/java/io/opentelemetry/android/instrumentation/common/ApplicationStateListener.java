/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common;

/**
 * Listener interface that is called whenever the instrumented application is brought to foreground
 * from the background, or vice versa.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface ApplicationStateListener {

    /**
     * Called whenever the application is brought to the foreground (i.e. first activity starts).
     */
    void onApplicationForegrounded();

    /** Called whenever the application is brought to the background (i.e. last activity stops). */
    void onApplicationBackgrounded();
}
