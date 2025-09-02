/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

/**
 * Listener interface that is called whenever the instrumented application is brought to foreground
 * from the background, or vice versa.
 *
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
interface ApplicationStateListener {
    /**
     * Called whenever the application is brought to the foreground (i.e. first activity starts).
     */
    fun onApplicationForegrounded()

    /**
     * Called whenever the application is brought to the background (i.e. last activity stops).
     */
    fun onApplicationBackgrounded()
}
