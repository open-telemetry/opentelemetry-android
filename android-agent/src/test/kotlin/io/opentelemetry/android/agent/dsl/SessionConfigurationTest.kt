/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(Incubating::class)
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
        assertEquals(15.minutes, otelConfig.sessionConfig.backgroundInactivityTimeout)
        assertEquals(4.hours, otelConfig.sessionConfig.maxLifetime)
        assertThat(otelConfig.sessionConfig.userInactivityTimeout).isNull()
    }

    @Test
    fun `user inactivity accepts finite positive values and can be disabled`() {
        otelConfig.session { userInactivityTimeout = 2.minutes }
        assertThat(otelConfig.sessionConfig.userInactivityTimeout).isEqualTo(2.minutes)
        otelConfig.session { userInactivityTimeout = null }
        assertThat(otelConfig.sessionConfig.userInactivityTimeout).isNull()
    }

    @Test
    fun `invalid user inactivity values warn and keep the previous value`() {
        val logger = Logger.getLogger(SessionConfiguration::class.java.name)
        val handler = mockk<Handler>(relaxed = true)
        logger.addHandler(handler)
        try {
            for (previous in listOf(null, 2.minutes)) {
                otelConfig.session { userInactivityTimeout = previous }
                for (invalid in listOf(Duration.ZERO, (-1).minutes, Duration.INFINITE, -Duration.INFINITE)) {
                    otelConfig.session { userInactivityTimeout = invalid }
                    assertThat(otelConfig.sessionConfig.userInactivityTimeout).isEqualTo(previous)
                }
            }
            verify(exactly = 8) {
                handler.publish(match { it.level == Level.WARNING && it.message.contains("positive and finite") })
            }
        } finally {
            logger.removeHandler(handler)
        }
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
