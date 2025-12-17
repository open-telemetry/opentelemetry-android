/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val ALTERNATIVE_BACKGROUND_TIMEOUT = 10.minutes
private val ALTERNATIVE_MAX_LIFETIME = 2.hours
private val DEFAULT_BACKGROUND_INACTIVITY_TIMEOUT = 15.minutes
private val DEFAULT_MAX_LIFETIME = 4.hours

/**
 * Validates [SessionConfig] data class functionality including defaults, constructors,
 * and edge cases.
 */
class SessionConfigTest {
    @Test
    fun `withDefaults should create config with expected default values`() {
        // When
        val config = SessionConfig.withDefaults()

        // Then
        assertAll(
            { assertEquals(DEFAULT_BACKGROUND_INACTIVITY_TIMEOUT, config.backgroundInactivityTimeout) },
            { assertEquals(DEFAULT_MAX_LIFETIME, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should accept valid values`() {
        // When
        val config =
            SessionConfig(
                backgroundInactivityTimeout = ALTERNATIVE_BACKGROUND_TIMEOUT,
                maxLifetime = ALTERNATIVE_MAX_LIFETIME,
            )

        // Then
        assertAll(
            { assertEquals(ALTERNATIVE_BACKGROUND_TIMEOUT, config.backgroundInactivityTimeout) },
            { assertEquals(ALTERNATIVE_MAX_LIFETIME, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should allow equal values for both timeouts`() {
        // Given
        val equalTimeout = 30.minutes

        // When
        val config =
            SessionConfig(
                backgroundInactivityTimeout = equalTimeout,
                maxLifetime = equalTimeout,
            )

        // Then
        assertAll(
            { assertEquals(equalTimeout, config.backgroundInactivityTimeout) },
            { assertEquals(equalTimeout, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should accept negative values`() {
        // Given
        val backgroundTimeout = (-5).minutes
        val maxLifetime = (-1).hours

        // When
        val config =
            SessionConfig(
                backgroundInactivityTimeout = backgroundTimeout,
                maxLifetime = maxLifetime,
            )

        // Then
        assertAll(
            { assertEquals(backgroundTimeout, config.backgroundInactivityTimeout) },
            { assertEquals(maxLifetime, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should accept zero values`() {
        // Given
        val timeout = 0.seconds

        // When
        val config =
            SessionConfig(
                backgroundInactivityTimeout = timeout,
                maxLifetime = timeout,
            )

        // Then
        assertAll(
            { assertEquals(timeout, config.backgroundInactivityTimeout) },
            { assertEquals(timeout, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should create instance with only backgroundInactivityTimeout specified`() {
        // Given
        val timeout = 10.minutes

        // When
        val config = SessionConfig(backgroundInactivityTimeout = timeout)

        // Then
        assertAll(
            { assertEquals(timeout, config.backgroundInactivityTimeout) },
            { assertEquals(DEFAULT_MAX_LIFETIME, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should create instance with only maxLifetime specified`() {
        // When
        val config = SessionConfig(maxLifetime = ALTERNATIVE_MAX_LIFETIME)

        // Then
        assertAll(
            { assertEquals(DEFAULT_BACKGROUND_INACTIVITY_TIMEOUT, config.backgroundInactivityTimeout) },
            { assertEquals(ALTERNATIVE_MAX_LIFETIME, config.maxLifetime) },
        )
    }

    @Test
    fun `should handle edge case durations`() {
        // Given - very small durations
        val smallBackgroundTimeout = 1.seconds
        val smallMaxLifetime = 2.seconds

        // When
        val smallConfig =
            SessionConfig(
                backgroundInactivityTimeout = smallBackgroundTimeout,
                maxLifetime = smallMaxLifetime,
            )

        // Then
        assertAll(
            { assertEquals(smallBackgroundTimeout, smallConfig.backgroundInactivityTimeout) },
            { assertEquals(smallMaxLifetime, smallConfig.maxLifetime) },
        )

        // Given - very large durations
        val largeBackgroundTimeout = 1000.hours
        val largeMaxLifetime = 2000.hours

        // When
        val largeConfig =
            SessionConfig(
                backgroundInactivityTimeout = largeBackgroundTimeout,
                maxLifetime = largeMaxLifetime,
            )

        // Then
        assertAll(
            { assertEquals(largeBackgroundTimeout, largeConfig.backgroundInactivityTimeout) },
            { assertEquals(largeMaxLifetime, largeConfig.maxLifetime) },
        )
    }
}
