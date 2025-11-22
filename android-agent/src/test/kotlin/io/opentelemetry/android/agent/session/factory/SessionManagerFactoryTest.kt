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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Verifies [SessionManagerFactory] correctly creates SessionProvider instances with proper
 * lifecycle integration and configuration handling.
 */
class SessionManagerFactoryTest {
    private lateinit var factory: SessionManagerFactory
    private lateinit var mockApplication: Application
    private lateinit var mockAppLifecycle: AppLifecycle

    @BeforeEach
    fun setUp() {
        factory = SessionManagerFactory()
        mockApplication = mockk(relaxed = true)
        mockAppLifecycle = mockk(relaxed = true)

        // Set up Services for testing
        val mockServices = mockk<Services>(relaxed = true)
        every { mockServices.appLifecycle } returns mockAppLifecycle
        Services.set(mockServices)
    }

    @AfterEach
    fun tearDown() {
        Services.set(null)
    }

    @Test
    fun `should create SessionProvider with default configuration`() {
        // Verifies that factory creates valid providers using default SessionConfig

        // Given
        val config = SessionConfig.withDefaults()

        // When
        val provider = factory.createSessionProvider(mockApplication, config)

        // Then
        assertNotNull(provider)
    }

    @Test
    fun `should create SessionProvider with custom configuration`() {
        // Verifies that factory accepts and uses custom SessionConfig values

        // Given
        val customConfig =
            SessionConfig(
                backgroundInactivityTimeout = 5.minutes,
                maxLifetime = 2.hours,
            )

        // When
        val provider = factory.createSessionProvider(mockApplication, customConfig)

        // Then
        assertNotNull(provider)
    }

    @Test
    fun `should register timeout handler with app lifecycle`() {
        // Verifies that factory integrates timeout handler with application lifecycle

        // Given
        val config = SessionConfig.withDefaults()

        // When
        factory.createSessionProvider(mockApplication, config)

        // Then
        verify { mockAppLifecycle.registerListener(any()) }
    }

    @Test
    fun `should create unique session providers for multiple calls`() {
        // Verifies that each factory invocation creates a distinct SessionProvider instance

        // Given
        val config = SessionConfig.withDefaults()

        // When
        val provider1 = factory.createSessionProvider(mockApplication, config)
        val provider2 = factory.createSessionProvider(mockApplication, config)

        // Then
        assertAll(
            { assertNotNull(provider1) },
            { assertNotNull(provider2) },
            { assertTrue(provider1 !== provider2) },
        )
    }

    @Test
    fun `created provider should return valid session IDs`() {
        // Verifies that factory-created providers generate non-empty session IDs

        // Given
        val config = SessionConfig.withDefaults()

        // When
        val provider = factory.createSessionProvider(mockApplication, config)
        val sessionId1 = provider.getSessionId()
        val sessionId2 = provider.getSessionId()

        // Then
        assertAll(
            { assertNotNull(sessionId1) },
            { assertTrue(sessionId1.isNotEmpty()) },
            { assertNotNull(sessionId2) },
            { assertTrue(sessionId2.isNotEmpty()) },
        )
    }

    @Test
    fun `created provider should initially have empty previous session ID`() {
        // Verifies that new providers start with empty previous session ID

        // Given
        val config = SessionConfig.withDefaults()

        // When
        val provider = factory.createSessionProvider(mockApplication, config)
        val previousSessionId = provider.getPreviousSessionId()

        // Then
        assertTrue(previousSessionId.isEmpty())
    }

    @Test
    fun `factory should work with minimal configuration`() {
        // Verifies that factory handles edge case configurations with minimal timeout values

        // Given
        val minimalConfig =
            SessionConfig(
                backgroundInactivityTimeout = 1.minutes,
                maxLifetime = 1.minutes,
            )

        // When
        val provider = factory.createSessionProvider(mockApplication, minimalConfig)

        // Then
        assertAll(
            { assertNotNull(provider) },
            { assertTrue(provider.getSessionId().isNotEmpty()) },
        )
    }

    @Test
    fun `factory should be extensible`() {
        // Verifies that factory can be extended to add custom behavior via inheritance

        // Given
        val customFactory =
            object : SessionManagerFactory() {
                var customCallCount = 0

                override fun createSessionProvider(
                    application: Application,
                    sessionConfig: SessionConfig,
                ): SessionProvider {
                    customCallCount++
                    return super.createSessionProvider(application, sessionConfig)
                }
            }
        val config = SessionConfig.withDefaults()

        // When
        customFactory.createSessionProvider(mockApplication, config)
        customFactory.createSessionProvider(mockApplication, config)

        // Then
        assertTrue(customFactory.customCallCount == 2)
    }
}
