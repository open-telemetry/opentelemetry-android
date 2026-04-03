/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

/**
 * Callback that is invoked at different points in the session lifecycle.
 */
interface SessionObserver {

    /**
     * Called after a session has started.
     */
    fun onSessionStarted(
        newSession: Session,
        previousSession: Session,
    )

    /**
     * Called after a session has ended.
     */
    fun onSessionEnded(session: Session)
}
