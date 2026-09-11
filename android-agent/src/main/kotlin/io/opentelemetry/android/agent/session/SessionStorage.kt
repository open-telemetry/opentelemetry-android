/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.session.Session

/**
 * Storage for sessions written by the agent's built-in session manager.
 *
 * The manager currently saves an empty session (ID `""`, start timestamp `-1`) during
 * initialization, then saves each newly generated session. It does not call [get] to restore
 * a session, so supplying persistent storage alone does not enable session restoration.
 *
 * Calls run synchronously on the calling thread, including during SDK initialization and
 * telemetry recording. Implementations must be thread-safe, return promptly, and handle their
 * own failures without throwing. Concurrent saves are not guaranteed to arrive in session
 * creation order. The manager does not provide a storage fallback or manage storage cleanup.
 */
@Incubating
interface SessionStorage {
    /** Returns the session held by this storage. The built-in manager does not currently read it. */
    fun get(): Session

    /** Saves [newSession], including the empty session supplied during initialization. */
    fun save(newSession: Session)
}
