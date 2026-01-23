/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session.factory

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.session.SessionProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Validates [SessionManagerFactory] correctly creates SessionProvider instances with proper
 * lifecycle integration and configuration handling.
 */
class SessionManagerFactoryTest {
    private lateinit var factory: SessionManagerFactory
    private lateinit var mockApplication: Application
    private lateinit var mockAppLifecycle: AppLifecycle

    @BeforeEach
    fun setUp() {
        mockApplication = mockk(relaxed = true)
        mockAppLifecycle = mockk(relaxed = true)

        // Set up Services for testing
        val mockServices = mockk<Services>(relaxed = true)
        every { mockServices.appLifecycle } returns mockAppLifecycle
        Services.set(mockServices)

        factory = SessionManagerFactory(mockApplication)
    }

    @AfterEach
    fun tearDown() {
        Services.set(null)
    }

    @Test
    fun `should create SessionProvider with default configuration`() {
        // Given
        val config = SessionConfig.withDefaults()

        // When
        val provider = factory.createSessionProvider(config)

        // Then
        assertThat(provider).isNotNull()
    }

    @Test
    fun `should create SessionProvider with different configuration`() {
        // Given
        val backgroundTimeout = 5.minutes
        val maxLifetime = 2.hours
        val config =
            SessionConfig(
                backgroundInactivityTimeout = backgroundTimeout,
                maxLifetime = maxLifetime,
            )

        // When
        val provider = factory.createSessionProvider(config)

        // Then
        assertThat(provider).isNotNull()
    }

    @Test
    fun `should register timeout handler with app lifecycle`() {
        // Given
        val config = SessionConfig.withDefaults()

        // When
        factory.createSessionProvider(config)

        // Then
        verify { mockAppLifecycle.registerListener(any()) }
    }

    @Test
    fun `should create unique session providers for multiple calls`() {
        // Given
        val config = SessionConfig.withDefaults()

        // When
        val provider1 = factory.createSessionProvider(config)
        val provider2 = factory.createSessionProvider(config)

        // Then
        assertAll(
            { assertThat(provider1).isNotNull() },
            { assertThat(provider2).isNotNull() },
            { assertThat(provider1 !== provider2).isTrue() },
        )
    }

    @Test
    fun `created provider should return valid session IDs`() {
        // Given
        val config = SessionConfig.withDefaults()

        // When
        val provider = factory.createSessionProvider(config)
        val sessionId1 = provider.getSessionId()
        val sessionId2 = provider.getSessionId()

        // Then
        assertAll(
            { assertThat(sessionId1).isNotNull().isNotEmpty() },
            { assertThat(sessionId2).isNotNull().isNotEmpty() },
        )
    }

    @Test
    fun `created provider should initially have empty previous session ID`() {
        // Given
        val config = SessionConfig.withDefaults()

        // When
        val provider = factory.createSessionProvider(config)
        val previousSessionId = provider.getPreviousSessionId()

        // Then
        assertThat(previousSessionId).isEmpty()
    }

    @Test
    fun `factory should work with minimal configuration`() {
        // Given
        val minimalTimeout = 1.minutes
        val minimalConfig =
            SessionConfig(
                backgroundInactivityTimeout = minimalTimeout,
                maxLifetime = minimalTimeout,
            )

        // When
        val provider = factory.createSessionProvider(minimalConfig)

        // Then
        assertAll(
            { assertThat(provider).isNotNull() },
            { assertThat(provider.getSessionId()).isNotEmpty() },
        )
    }

    @Test
    fun `factory should be extensible`() {
        // Given
        val expectedCallCount = 2
        val testFactory =
            object : SessionManagerFactory(mockApplication) {
                var callCount = 0

                override fun createSessionProvider(sessionConfig: SessionConfig): SessionProvider {
                    callCount++
                    return super.createSessionProvider(sessionConfig)
                }
            }
        val config = SessionConfig.withDefaults()

        // When
        testFactory.createSessionProvider(config)
        testFactory.createSessionProvider(config)

        // Then
        assertThat(testFactory.callCount).isEqualTo(expectedCallCount)
    }
}
