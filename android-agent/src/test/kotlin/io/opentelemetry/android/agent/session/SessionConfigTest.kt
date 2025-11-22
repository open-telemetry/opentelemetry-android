/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val DEFAULT_BACKGROUND_TIMEOUT = 15.minutes
private val DEFAULT_MAX_LIFETIME = 4.hours

/**
 * Tests the functionality of the [SessionConfig] data class.
 */
class SessionConfigTest {
    @Test
    fun `withDefaults should create config with expected default values`() {
        // When
        val config = SessionConfig.withDefaults()

        // Then
        assertAll(
            { assertEquals(DEFAULT_BACKGROUND_TIMEOUT, config.backgroundInactivityTimeout) },
            { assertEquals(DEFAULT_MAX_LIFETIME, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should accept valid custom values`() {
        // When
        val backgroundTimeout = 10.minutes
        val maxLifetime = 2.hours
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
    fun `constructor should allow equal values for both timeouts`() {
        // When
        val config =
            SessionConfig(
                backgroundInactivityTimeout = 30.minutes,
                maxLifetime = 30.minutes,
            )

        // Then
        assertAll(
            { assertEquals(30.minutes, config.backgroundInactivityTimeout) },
            { assertEquals(30.minutes, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should accept negative values`() {
        // When
        val config =
            SessionConfig(
                backgroundInactivityTimeout = (-5).minutes,
                maxLifetime = (-1).hours,
            )

        // Then
        assertAll(
            { assertEquals((-5).minutes, config.backgroundInactivityTimeout) },
            { assertEquals((-1).hours, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should accept zero values`() {
        // When
        val config =
            SessionConfig(
                backgroundInactivityTimeout = 0.seconds,
                maxLifetime = 0.seconds,
            )

        // Then
        assertAll(
            { assertEquals(0.seconds, config.backgroundInactivityTimeout) },
            { assertEquals(0.seconds, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should create instance with only backgroundInactivityTimeout specified`() {
        // When
        val backgroundTimeout = 10.minutes
        val config = SessionConfig(backgroundInactivityTimeout = backgroundTimeout)

        // Then
        assertAll(
            { assertEquals(backgroundTimeout, config.backgroundInactivityTimeout) },
            { assertEquals(DEFAULT_MAX_LIFETIME, config.maxLifetime) }, // default value
        )
    }

    @Test
    fun `constructor should create instance with only maxLifetime specified`() {
        // When
        val maxLifetime = 2.hours
        val config = SessionConfig(maxLifetime = maxLifetime)

        // Then
        assertAll(
            { assertEquals(DEFAULT_BACKGROUND_TIMEOUT, config.backgroundInactivityTimeout) }, // default value
            { assertEquals(maxLifetime, config.maxLifetime) },
        )
    }

    @Test
    fun `toString should include both timeout values`() {
        // Given
        val config =
            SessionConfig(
                backgroundInactivityTimeout = 20.minutes,
                maxLifetime = 3.hours,
            )

        // When
        val toString = config.toString()

        // Then
        assertAll(
            { assertTrue(toString.contains("SessionConfig")) },
            { assertTrue(toString.contains("backgroundInactivityTimeout=20m")) },
            { assertTrue(toString.contains("maxLifetime=3h")) },
        )
    }

    @Test
    fun `equals should return true for identical configurations`() {
        // Given
        val backgroundTimeout = 10.minutes
        val maxLifetime = 2.hours
        val config1 =
            SessionConfig(
                backgroundInactivityTimeout = backgroundTimeout,
                maxLifetime = maxLifetime,
            )
        val config2 =
            SessionConfig(
                backgroundInactivityTimeout = backgroundTimeout,
                maxLifetime = maxLifetime,
            )

        // Then
        assertAll(
            { assertEquals(config1, config2) },
            { assertEquals(config2, config1) },
        )
    }

    @Test
    fun `equals should return false for different backgroundInactivityTimeout`() {
        // Given
        val config1 = SessionConfig(backgroundInactivityTimeout = 10.minutes)
        val config2 = SessionConfig(backgroundInactivityTimeout = DEFAULT_BACKGROUND_TIMEOUT)

        // Then
        assertNotEquals(config1, config2)
    }

    @Test
    fun `equals should return false for different maxLifetime`() {
        // Given
        val config1 = SessionConfig(maxLifetime = 2.hours)
        val config2 = SessionConfig(maxLifetime = DEFAULT_MAX_LIFETIME)

        // Then
        assertNotEquals(config1, config2)
    }

    @Test
    fun `hashCode should be equal for identical configurations`() {
        // Given
        val backgroundTimeout = 10.minutes
        val maxLifetime = 2.hours
        val config1 =
            SessionConfig(
                backgroundInactivityTimeout = backgroundTimeout,
                maxLifetime = maxLifetime,
            )
        val config2 =
            SessionConfig(
                backgroundInactivityTimeout = backgroundTimeout,
                maxLifetime = maxLifetime,
            )

        // Then
        assertEquals(config1.hashCode(), config2.hashCode())
    }

    @Test
    fun `hashCode should be different for different configurations`() {
        // Given
        val config1 = SessionConfig(backgroundInactivityTimeout = 10.minutes)
        val config2 = SessionConfig(backgroundInactivityTimeout = DEFAULT_BACKGROUND_TIMEOUT)

        // Then
        assertNotEquals(config1.hashCode(), config2.hashCode())
    }

    @Test
    fun `withDefaults should always create equal instances`() {
        // When
        val config1 = SessionConfig.withDefaults()
        val config2 = SessionConfig.withDefaults()

        // Then
        assertAll(
            { assertEquals(config1, config2) },
            { assertEquals(config1.hashCode(), config2.hashCode()) },
        )
    }

    @Test
    fun `data class copy should work correctly`() {
        // Given
        val originalBackgroundTimeout = 20.minutes
        val originalMaxLifetime = 3.hours
        val original =
            SessionConfig(
                backgroundInactivityTimeout = originalBackgroundTimeout,
                maxLifetime = originalMaxLifetime,
            )

        // When - copy with modified backgroundInactivityTimeout
        val modifiedBackgroundTimeout = 25.minutes
        val copied1 = original.copy(backgroundInactivityTimeout = modifiedBackgroundTimeout)

        // Then
        assertAll(
            { assertEquals(modifiedBackgroundTimeout, copied1.backgroundInactivityTimeout) },
            { assertEquals(originalMaxLifetime, copied1.maxLifetime) }, // unchanged
        )

        // When - copy with modified maxLifetime
        val modifiedMaxLifetime = 5.hours
        val copied2 = original.copy(maxLifetime = modifiedMaxLifetime)

        // Then
        assertAll(
            { assertEquals(originalBackgroundTimeout, copied2.backgroundInactivityTimeout) }, // unchanged
            { assertEquals(modifiedMaxLifetime, copied2.maxLifetime) },
        )

        // When - copy with no changes
        val copied3 = original.copy()

        // Then
        assertAll(
            { assertEquals(copied3, original) },
            { assertNotSame(copied3, original) }, // different instances
        )
    }

    @Test
    fun `data class component functions should work correctly`() {
        // Given
        val config =
            SessionConfig(
                backgroundInactivityTimeout = 12.minutes,
                maxLifetime = 6.hours,
            )

        // When
        val (component1, component2) = config

        // Then
        assertAll(
            { assertEquals(12.minutes, component1) },
            { assertEquals(6.hours, component2) },
        )
    }

    @Test
    fun `companion object withDefaults should be accessible`() {
        // When & Then
        assertAll(
            { assertNotNull(SessionConfig.Companion.withDefaults()) },
            { assertNotNull(SessionConfig.withDefaults()) },
            { assertEquals(SessionConfig.Companion.withDefaults(), SessionConfig.withDefaults()) },
        )
    }

    @Test
    fun `should handle edge case durations`() {
        // When - very small durations
        val smallConfig =
            SessionConfig(
                backgroundInactivityTimeout = 1.seconds,
                maxLifetime = 2.seconds,
            )

        // Then
        assertAll(
            { assertEquals(1.seconds, smallConfig.backgroundInactivityTimeout) },
            { assertEquals(2.seconds, smallConfig.maxLifetime) },
        )

        // When - very large durations
        val largeConfig =
            SessionConfig(
                backgroundInactivityTimeout = 1000.hours,
                maxLifetime = 2000.hours,
            )

        // Then
        assertAll(
            { assertEquals(1000.hours, largeConfig.backgroundInactivityTimeout) },
            { assertEquals(2000.hours, largeConfig.maxLifetime) },
        )
    }
}
