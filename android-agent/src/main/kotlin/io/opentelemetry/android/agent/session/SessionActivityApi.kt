/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.extensions.ApiExtension

/**
 * Manual API to tell the session manager whether the user is actively using the app.
 *
 * A session expires after a period of user inactivity. By default, the agent infers activity from
 * the app lifecycle (the user is active while the app is in the foreground and inactive once it
 * goes to the background). That default doesn't fit every app, for example a media player that is
 * still being used from a notification while backgrounded. This API lets the app report the state
 * itself.
 *
 * This API is the single source of truth for user activity. The default lifecycle based inference
 * writes through it as well, so the last write wins regardless of where it came from.
 *
 * Obtain it with [sessions] or `openTelemetryRum.getExtension(SessionActivityApi::class.java)`.
 */
@Incubating
interface SessionActivityApi : ApiExtension {
    override val type: Class<out ApiExtension>
        get() = SessionActivityApi::class.java

    /**
     * Signals that the user is actively using the app. If the inactivity timeout has already
     * elapsed, the next telemetry item starts a new session.
     */
    fun userActive()

    /**
     * Signals that the user stopped using the app. The session expires if [userActive] isn't
     * called again before the configured inactivity timeout.
     */
    fun userInactive()
}

/**
 * Convenience accessor for Kotlin callers. Returns null when the sessions extension isn't
 * available in this [OpenTelemetryRum] instance.
 */
@Incubating
fun OpenTelemetryRum.sessions(): SessionActivityApi? = getExtension(SessionActivityApi::class.java)
