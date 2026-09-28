/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.mockk
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.agent.session.InMemorySessionStorage
import io.opentelemetry.android.agent.session.SessionStorage
import io.opentelemetry.android.session.SessionObserver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(Incubating::class)
internal class SessionStorageConfigurationTest {
    private val config = OpenTelemetryConfiguration(clock = FakeClock(), instrumentationLoader = FakeInstrumentationLoader())

    @Test
    fun `each configuration has its own default in-memory storage`() {
        assertThat(config.sessionConfig.sessionStorage).isInstanceOf(InMemorySessionStorage::class.java)
        assertThat(config.sessionConfig.sessionStorage).isNotSameAs(SessionConfiguration().sessionStorage)
        assertThat(config.sessionConfig.backgroundInactivityTimeout).isEqualTo(15.minutes)
        assertThat(config.sessionConfig.maxLifetime).isEqualTo(4.hours)
    }

    @Test
    fun `last storage wins without replacing other session configuration`() {
        val first = mockk<SessionStorage>()
        val last = mockk<SessionStorage>()
        val observer = mockk<SessionObserver>()
        config.session {
            storage(first)
            backgroundInactivityTimeout = 3.minutes
            maxLifetime = 2.hours
            observers(observer)
        }
        config.session { storage(last) }

        assertThat(config.sessionConfig.sessionStorage).isSameAs(last)
        assertThat(config.sessionConfig.backgroundInactivityTimeout).isEqualTo(3.minutes)
        assertThat(config.sessionConfig.maxLifetime).isEqualTo(2.hours)
        assertThat(config.sessionConfig.getObservers()).containsExactly(observer)
    }
}
