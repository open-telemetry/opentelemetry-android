/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.mockk
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class SessionConfigurationTest {
    private lateinit var otelConfig: OpenTelemetryConfiguration

    @Before
    fun setUp() {
        otelConfig =
            OpenTelemetryConfiguration(
                clock = FakeClock(),
                instrumentationLoader = FakeInstrumentationLoader(),
            )
    }

    @Test
    fun testDefaults() {
        assertEquals(true, otelConfig.sessionConfig.includeSessionId)
        assertEquals(true, otelConfig.rumConfig.shouldIncludeSessionId())
        assertEquals(15.minutes, otelConfig.sessionConfig.backgroundInactivityTimeout)
        assertEquals(4.hours, otelConfig.sessionConfig.maxLifetime)
    }

    @Test
    fun testDisableSessionIdViaSessionConfig() {
        val otelConfig =
            otelConfig.apply {
                session {
                    includeSessionId = false
                }
            }
        assertEquals(false, otelConfig.sessionConfig.includeSessionId)
        assertEquals(false, otelConfig.rumConfig.shouldIncludeSessionId())
    }

    @Test
    fun testDisableSessionIdDirectly() {
        val otelConfig =
            otelConfig.apply {
                disableSessionId()
            }
        assertEquals(false, otelConfig.rumConfig.shouldIncludeSessionId())
    }

    @Test
    fun testDisableScreenAttributesDirectly() {
        assertEquals(true, otelConfig.rumConfig.shouldIncludeScreenAttributes())
        val otelConfig =
            otelConfig.apply {
                disableScreenAttributes()
            }
        assertEquals(false, otelConfig.rumConfig.shouldIncludeScreenAttributes())
    }

    @Test
    fun testOverride() {
        val customTimeout = 30.minutes
        val customLifetime = 2.hours
        val otelConfig =
            otelConfig.apply {
                session {
                    backgroundInactivityTimeout = customTimeout
                    maxLifetime = customLifetime
                }
            }
        assertEquals(customTimeout, otelConfig.sessionConfig.backgroundInactivityTimeout)
        assertEquals(customLifetime, otelConfig.sessionConfig.maxLifetime)
    }

    @Test
    fun `can add session observers`() {
        val o1: SessionObserver = mockk()
        val o2: SessionObserver = mockk()
        val otelConfig =
            otelConfig.apply {
                session {
                    observers(o1, o2)
                }
            }
        assertThat(otelConfig.sessionConfig.getObservers()).containsExactly(o1, o2)
    }
}
