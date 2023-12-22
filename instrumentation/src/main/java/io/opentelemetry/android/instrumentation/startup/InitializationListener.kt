/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

/**
 * Provides callbacks to know the sate of the initialization.
 */
interface InitializationListener {
    /**
     * Called when the RUM initialization starts.
     */
    fun onStart()

    /**
     * Called when the RUM initialization ends.
     */
    fun onEnd()
}
