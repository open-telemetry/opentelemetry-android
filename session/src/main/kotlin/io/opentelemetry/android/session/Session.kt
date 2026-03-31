/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

/**
 * Provides information about the current session.
 */
interface Session {

    /**
     * The session ID.
     */
    val id: String

    /**
     * The timestamp at which the session was started.
     */
    val startTimestamp: Long
}
