/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Configures session management behavior in the OpenTelemetry Android SDK.
 *
 * Sessions provide a way to group related telemetry data (spans, logs, metrics) that occur during
 * a logical user interaction or application usage period. This configuration controls when sessions
 * expire and new sessions are created.
 *
 * ## Session Lifecycle
 *
 * A session can end due to two conditions:
 * 1. **Background Inactivity**: When the app goes to background and remains inactive for longer than [backgroundInactivityTimeout]
 * 2. **Maximum Lifetime**: When a session has been active for longer than [maxLifetime], regardless of app state
 *
 * When a session ends, a new session will be created on the next telemetry operation, and the previous
 * session ID will be tracked for correlation purposes.
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Use default configuration (15 minutes background timeout, 4 hours max lifetime)
 * val config = SessionConfig.withDefaults()
 *
 * // Custom configuration for shorter sessions
 * val shortSessionConfig = SessionConfig(
 *     backgroundInactivityTimeout = 5.minutes,
 *     maxLifetime = 1.hours
 * )
 * ```
 *
 * @param backgroundInactivityTimeout duration of app backgrounding after which the session expires.
 *   Default is 15 minutes, meaning if the app stays in background for 15+ minutes, the current session
 *   ends and a new one will be created when the app becomes active again.
 *
 * @param maxLifetime maximum duration a session can remain active regardless of app activity.
 *   Default is 4 hours, meaning even if the app stays in foreground continuously, sessions will
 *   rotate every 4 hours to prevent unbounded session growth and ensure fresh session context.
 *
 * @see io.opentelemetry.android.agent.session.SessionManager
 * @see io.opentelemetry.android.session.SessionProvider
 */
data class SessionConfig(
    val backgroundInactivityTimeout: Duration = 15.minutes,
    val maxLifetime: Duration = 4.hours,
) {
    companion object {
        /**
         * Creates a SessionConfig with default values.
         *
         * Default configuration:
         * - Background inactivity timeout: 15 minutes
         * - Maximum session lifetime: 4 hours
         *
         * These defaults balance session continuity with memory efficiency and provide
         * reasonable session boundaries for most mobile applications.
         *
         * @return a new SessionConfig instance with default timeout values.
         */
        @JvmStatic
        fun withDefaults(): SessionConfig = SessionConfig()
    }
}
