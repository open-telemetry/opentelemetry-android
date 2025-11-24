/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class SessionConfigurationTest {
    @Test
    fun testDefaults() {
        val otelConfig = OpenTelemetryConfiguration()
        assertEquals(15.minutes, otelConfig.sessionConfig.backgroundInactivityTimeout)
        assertEquals(4.hours, otelConfig.sessionConfig.maxLifetime)
    }

    @Test
    fun testOverride() {
        val customTimeout = 30.minutes
        val customLifetime = 2.hours
        val otelConfig =
            OpenTelemetryConfiguration().apply {
                session {
                    backgroundInactivityTimeout = customTimeout
                    maxLifetime = customLifetime
                }
            }
        assertEquals(customTimeout, otelConfig.sessionConfig.backgroundInactivityTimeout)
        assertEquals(customLifetime, otelConfig.sessionConfig.maxLifetime)
    }
}
