/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OpenTelemetryDslMarker
class SessionConfiguration(
    var backgroundInactivityTimeout: Duration = 15.minutes,
    var maxLifetime: Duration = 4.hours,
)
