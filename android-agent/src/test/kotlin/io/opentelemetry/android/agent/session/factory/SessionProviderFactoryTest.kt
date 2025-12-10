/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session.factory

import android.app.Application
import io.mockk.mockk
import io.opentelemetry.android.agent.session.SessionConfig
import io.opentelemetry.android.session.SessionProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Validates [SessionProviderFactory] interface contract and extensibility.
 */
class SessionProviderFactoryTest {
    @Test
    fun `implementation should follow factory contract`() {
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
            { assertThat(provider).isNotNull() },
            { assertThat(provider).isEqualTo(mockProvider) },
        )
    }

    @Test
    fun `implementation can create different providers based on config`() {
        // Given
        val sessionId1 = "session-1"
        val sessionId2 = "session-2"
        val lifetimeThreshold = 2.hours
        val factory =
            object : SessionProviderFactory {
                override fun createSessionProvider(
                    application: Application,
                    sessionConfig: SessionConfig,
                ): SessionProvider {
                    val sessionId = if (sessionConfig.maxLifetime < lifetimeThreshold) sessionId1 else sessionId2
                    return object : SessionProvider {
                        override fun getSessionId(): String = sessionId

                        override fun getPreviousSessionId(): String = SessionProvider.NO_SESSION_ID
                    }
                }
            }
        val mockApplication = mockk<Application>(relaxed = true)
        val shortLifetime = 1.hours
        val longLifetime = 4.hours
        val shortConfig = SessionConfig(maxLifetime = shortLifetime)
        val longConfig = SessionConfig(maxLifetime = longLifetime)

        // When
        val shortProvider = factory.createSessionProvider(mockApplication, shortConfig)
        val longProvider = factory.createSessionProvider(mockApplication, longConfig)

        // Then
        assertAll(
            { assertThat(shortProvider.getSessionId()).isEqualTo(sessionId1) },
            { assertThat(longProvider.getSessionId()).isEqualTo(sessionId2) },
        )
    }

    @Test
    fun `factory can be used for testing`() {
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
            { assertThat(provider.getSessionId()).isEqualTo(testSessionId) },
            { assertThat(provider.getPreviousSessionId()).isEqualTo(testPreviousSessionId) },
        )
    }

    @Test
    fun `factory should accept various session configurations`() {
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

        // Given
        val firstBackgroundTimeout = 5.minutes
        val firstMaxLifetime = 1.hours

        // When
        val config1 = SessionConfig(backgroundInactivityTimeout = firstBackgroundTimeout, maxLifetime = firstMaxLifetime)
        factory.createSessionProvider(mockApplication, config1)

        // Then
        assertAll(
            { assertThat(factory.lastConfig).isNotNull() },
            { assertThat(factory.lastConfig?.backgroundInactivityTimeout).isEqualTo(firstBackgroundTimeout) },
            { assertThat(factory.lastConfig?.maxLifetime).isEqualTo(firstMaxLifetime) },
        )

        // Given
        val secondBackgroundTimeout = 20.minutes
        val secondMaxLifetime = 5.hours

        // When
        val config2 = SessionConfig(backgroundInactivityTimeout = secondBackgroundTimeout, maxLifetime = secondMaxLifetime)
        factory.createSessionProvider(mockApplication, config2)

        // Then
        assertAll(
            { assertThat(factory.lastConfig?.backgroundInactivityTimeout).isEqualTo(secondBackgroundTimeout) },
            { assertThat(factory.lastConfig?.maxLifetime).isEqualTo(secondMaxLifetime) },
        )
    }

    @Test
    fun `factory implementation can track creation count`() {
        // Given
        var creationCount = 0
        val expectedCreationCount = 3
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
        assertThat(creationCount).isEqualTo(expectedCreationCount)
    }

    @Test
    fun `factory can provide stateful session providers`() {
        // Given
        val expectedSessionLabelPrefix = "session-"
        val factory =
            object : SessionProviderFactory {
                private var counter = 0

                override fun createSessionProvider(
                    application: Application,
                    sessionConfig: SessionConfig,
                ): SessionProvider {
                    val providerNumber = counter++
                    return object : SessionProvider {
                        override fun getSessionId(): String = "$expectedSessionLabelPrefix$providerNumber"

                        override fun getPreviousSessionId(): String = SessionProvider.NO_SESSION_ID
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
            { assertThat(provider1.getSessionId()).isEqualTo("${expectedSessionLabelPrefix}0") },
            { assertThat(provider2.getSessionId()).isEqualTo("${expectedSessionLabelPrefix}1") },
            { assertThat(provider3.getSessionId()).isEqualTo("${expectedSessionLabelPrefix}2") },
        )
    }
}
