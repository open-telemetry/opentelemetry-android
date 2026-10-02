/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.mockk
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
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
        listOf(Duration.ZERO, (-1).minutes, Duration.INFINITE).forEach { invalid ->
            assertThatThrownBy { otelConfig.session { userInactivityTimeout = invalid } }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
        assertThat(otelConfig.sessionConfig.userInactivityTimeout).isEqualTo(2.minutes)
        otelConfig.session { userInactivityTimeout = null }
        assertThat(otelConfig.sessionConfig.userInactivityTimeout).isNull()
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
