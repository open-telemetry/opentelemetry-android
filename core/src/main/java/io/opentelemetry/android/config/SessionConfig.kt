/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

data class SessionConfig(
    val backgroundInactivityTimeout: Duration = 15.minutes,
    val maxLifetime: Duration = 4.hours,
    val ratio: Double? = null,
) {
    constructor(
        backgroundInactivityTimeout: Int = 15,
        maxLifetime: Int = 4,
        ratio: Double? = null,
    ) : this(
        backgroundInactivityTimeout = backgroundInactivityTimeout.minutes,
        maxLifetime = maxLifetime.hours,
        ratio = ratio,
    )

    companion object {
        @JvmStatic
        fun withDefaults(): SessionConfig = SessionConfig(15.minutes, 4.hours)
    }
}
