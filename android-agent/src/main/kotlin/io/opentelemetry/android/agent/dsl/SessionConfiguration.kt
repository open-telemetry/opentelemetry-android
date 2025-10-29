/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Type-safe config DSL that controls how sessions should behave.
 */
@OpenTelemetryDslMarker
class SessionConfiguration internal constructor() {
    /**
     * The maximum duration which a session can remain open in the background before it
     * automatically expires.
     */
    var backgroundInactivityTimeout: Duration = 15.minutes

    /**
     * The maximum duration which a session can remain open before it automatically expires.
     */
    var maxLifetime: Duration = 4.hours
}
