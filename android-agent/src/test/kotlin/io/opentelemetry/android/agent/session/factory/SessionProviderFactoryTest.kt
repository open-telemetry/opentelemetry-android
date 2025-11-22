/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session.factory

import android.app.Application
import io.mockk.mockk
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.session.SessionProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Verifies [SessionProviderFactory] interface contract and extensibility for custom implementations.
 */
class SessionProviderFactoryTest {
    @Test
    fun `custom implementation should follow factory contract`() {
        // Verifies that custom factory implementations correctly return SessionProvider instances

        // Given
        val mockProvider = mockk<SessionProvider>(relaxed = true)
        val factory =
            object : SessionProviderFactory {
                override fun createSessionProvider(
                    application: Application,
                    sessionConfig: SessionConfig,
                ): SessionProvider = mockProvider
            }
        val mockApplication = mockk<Application>(relaxed = true)
        val config = SessionConfig.withDefaults()

        // When
        val provider = factory.createSessionProvider(mockApplication, config)

        // Then
        assertAll(
            { assertNotNull(provider) },
            { assertEquals(mockProvider, provider) },
        )
    }

    @Test
    fun `custom implementation can create different providers based on config`() {
        // Verifies that factories can use SessionConfig to create different provider implementations

        // Given
        val sessionId1 = "session-1"
        val sessionId2 = "session-2"
        val factory =
            object : SessionProviderFactory {
                override fun createSessionProvider(
                    application: Application,
                    sessionConfig: SessionConfig,
                ): SessionProvider {
                    val sessionId = if (sessionConfig.maxLifetime < 2.hours) sessionId1 else sessionId2
                    return object : SessionProvider {
                        override fun getSessionId(): String = sessionId

                        override fun getPreviousSessionId(): String = ""
                    }
                }
            }
        val mockApplication = mockk<Application>(relaxed = true)
        val shortConfig = SessionConfig(maxLifetime = 1.hours)
        val longConfig = SessionConfig(maxLifetime = 4.hours)

        // When
        val shortProvider = factory.createSessionProvider(mockApplication, shortConfig)
        val longProvider = factory.createSessionProvider(mockApplication, longConfig)

        // Then
        assertAll(
            { assertEquals(sessionId1, shortProvider.getSessionId()) },
            { assertEquals(sessionId2, longProvider.getSessionId()) },
        )
    }

    @Test
    fun `factory can be used for testing with custom providers`() {
        // Verifies that factory pattern enables test doubles with controlled session IDs

        // Given
        val testSessionId = "test-session-id"
        val testPreviousSessionId = "test-previous-session-id"
        val factory =
            object : SessionProviderFactory {
                override fun createSessionProvider(
                    application: Application,
                    sessionConfig: SessionConfig,
                ): SessionProvider =
                    object : SessionProvider {
                        override fun getSessionId(): String = testSessionId

                        override fun getPreviousSessionId(): String = testPreviousSessionId
                    }
            }
        val mockApplication = mockk<Application>(relaxed = true)
        val config = SessionConfig.withDefaults()

        // When
        val provider = factory.createSessionProvider(mockApplication, config)

        // Then
        assertAll(
            { assertEquals(testSessionId, provider.getSessionId()) },
            { assertEquals(testPreviousSessionId, provider.getPreviousSessionId()) },
        )
    }

    @Test
    fun `factory should accept various session configurations`() {
        // Verifies that factories receive and can inspect SessionConfig parameters

        // Given
        val factory =
            object : SessionProviderFactory {
                var lastConfig: SessionConfig? = null

                override fun createSessionProvider(
                    application: Application,
                    sessionConfig: SessionConfig,
                ): SessionProvider {
                    lastConfig = sessionConfig
                    return SessionProvider.getNoop()
                }
            }
        val mockApplication = mockk<Application>(relaxed = true)

        // When
        val config1 = SessionConfig(backgroundInactivityTimeout = 5.minutes, maxLifetime = 1.hours)
        factory.createSessionProvider(mockApplication, config1)

        // Then
        assertAll(
            { assertNotNull(factory.lastConfig) },
            { assertEquals(5.minutes, factory.lastConfig?.backgroundInactivityTimeout) },
            { assertEquals(1.hours, factory.lastConfig?.maxLifetime) },
        )

        // When
        val config2 = SessionConfig(backgroundInactivityTimeout = 20.minutes, maxLifetime = 5.hours)
        factory.createSessionProvider(mockApplication, config2)

        // Then
        assertAll(
            { assertEquals(20.minutes, factory.lastConfig?.backgroundInactivityTimeout) },
            { assertEquals(5.hours, factory.lastConfig?.maxLifetime) },
        )
    }

    @Test
    fun `factory implementation can track creation count`() {
        // Verifies that factory methods are called for each createSessionProvider invocation

        // Given
        var creationCount = 0
        val factory =
            object : SessionProviderFactory {
                override fun createSessionProvider(
                    application: Application,
                    sessionConfig: SessionConfig,
                ): SessionProvider {
                    creationCount++
                    return SessionProvider.getNoop()
                }
            }
        val mockApplication = mockk<Application>(relaxed = true)
        val config = SessionConfig.withDefaults()

        // When
        factory.createSessionProvider(mockApplication, config)
        factory.createSessionProvider(mockApplication, config)
        factory.createSessionProvider(mockApplication, config)

        // Then
        assertEquals(3, creationCount)
    }

    @Test
    fun `factory can provide stateful session providers`() {
        // Verifies that factories can maintain state across multiple provider creations

        // Given
        val factory =
            object : SessionProviderFactory {
                private var counter = 0

                override fun createSessionProvider(
                    application: Application,
                    sessionConfig: SessionConfig,
                ): SessionProvider {
                    val providerNumber = counter++
                    return object : SessionProvider {
                        override fun getSessionId(): String = "session-$providerNumber"

                        override fun getPreviousSessionId(): String = ""
                    }
                }
            }
        val mockApplication = mockk<Application>(relaxed = true)
        val config = SessionConfig.withDefaults()

        // When
        val provider1 = factory.createSessionProvider(mockApplication, config)
        val provider2 = factory.createSessionProvider(mockApplication, config)
        val provider3 = factory.createSessionProvider(mockApplication, config)

        // Then
        assertAll(
            { assertEquals("session-0", provider1.getSessionId()) },
            { assertEquals("session-1", provider2.getSessionId()) },
            { assertEquals("session-2", provider3.getSessionId()) },
        )
    }
}
