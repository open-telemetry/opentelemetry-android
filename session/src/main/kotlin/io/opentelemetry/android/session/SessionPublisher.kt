/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

/**
 * Allows registration of callbacks that are invoked when the session changes.
 */
fun interface SessionPublisher {

    /**
     * Adds an observer that is invoked when the session changes.
     */
    fun addObserver(observer: SessionObserver)
}
