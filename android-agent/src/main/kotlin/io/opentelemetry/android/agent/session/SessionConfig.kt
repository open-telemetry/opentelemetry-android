/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class SessionConfig(
    val backgroundInactivityTimeout: Duration = 15.minutes,
    val maxLifetime: Duration = 4.hours,
) {
    companion object {
        @JvmStatic
        fun withDefaults(): SessionConfig = SessionConfig()
    }
}
