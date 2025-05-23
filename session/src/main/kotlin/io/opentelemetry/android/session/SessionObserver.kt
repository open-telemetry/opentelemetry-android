/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

interface SessionObserver {
    fun onSessionStarted(
        newSession: Session,
        previousSession: Session,
    )

    fun onSessionEnded(session: Session)
}
