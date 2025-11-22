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

/**
 * Verifies [SessionConfig] data class functionality including defaults, constructors, equality,
 * and edge cases.
 */
class SessionConfigTest {
    @Test
    fun `withDefaults should create config with expected default values`() {
        // Verifies that withDefaults factory method creates configuration with expected timeout values

        // When
        val config = SessionConfig.withDefaults()

        // Then
        assertAll(
            { assertEquals(15.minutes, config.backgroundInactivityTimeout) },
            { assertEquals(4.hours, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should accept valid custom values`() {
        // Verifies that custom timeout values can be provided to the constructor

        // When
        val config =
            SessionConfig(
                backgroundInactivityTimeout = 10.minutes,
                maxLifetime = 2.hours,
            )

        // Then
        assertAll(
            { assertEquals(10.minutes, config.backgroundInactivityTimeout) },
            { assertEquals(2.hours, config.maxLifetime) },
        )
    }

    @Test
    fun `constructor should allow equal values for both timeouts`() {
        // Verifies that equal timeout values are accepted as valid configuration

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
        // Verifies that negative duration values are accepted by the constructor

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
        // Verifies that zero duration values are accepted as valid configuration

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
        // Verifies that maxLifetime defaults correctly when only backgroundInactivityTimeout is provided

        // When
        val config = SessionConfig(backgroundInactivityTimeout = 10.minutes)

        // Then
        assertAll(
            { assertEquals(10.minutes, config.backgroundInactivityTimeout) },
            { assertEquals(4.hours, config.maxLifetime) }, // default value
        )
    }

    @Test
    fun `constructor should create instance with only maxLifetime specified`() {
        // Verifies that backgroundInactivityTimeout defaults correctly when only maxLifetime is provided

        // When
        val config = SessionConfig(maxLifetime = 2.hours)

        // Then
        assertAll(
            { assertEquals(15.minutes, config.backgroundInactivityTimeout) }, // default value
            { assertEquals(2.hours, config.maxLifetime) },
        )
    }

    @Test
    fun `toString should include both timeout values`() {
        // Verifies that toString representation includes all configuration parameters

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
        // Verifies that data class equality works correctly for identical configurations

        // Given
        val config1 =
            SessionConfig(
                backgroundInactivityTimeout = 10.minutes,
                maxLifetime = 2.hours,
            )
        val config2 =
            SessionConfig(
                backgroundInactivityTimeout = 10.minutes,
                maxLifetime = 2.hours,
            )

        // Then
        assertAll(
            { assertEquals(config1, config2) },
            { assertEquals(config2, config1) },
        )
    }

    @Test
    fun `equals should return false for different backgroundInactivityTimeout`() {
        // Verifies that configurations with different backgroundInactivityTimeout are not equal

        // Given
        val config1 = SessionConfig(backgroundInactivityTimeout = 10.minutes)
        val config2 = SessionConfig(backgroundInactivityTimeout = 15.minutes)

        // Then
        assertNotEquals(config1, config2)
    }

    @Test
    fun `equals should return false for different maxLifetime`() {
        // Verifies that configurations with different maxLifetime are not equal

        // Given
        val config1 = SessionConfig(maxLifetime = 2.hours)
        val config2 = SessionConfig(maxLifetime = 4.hours)

        // Then
        assertNotEquals(config1, config2)
    }

    @Test
    fun `hashCode should be equal for identical configurations`() {
        // Verifies that hashCode contract is maintained for equal objects

        // Given
        val config1 =
            SessionConfig(
                backgroundInactivityTimeout = 10.minutes,
                maxLifetime = 2.hours,
            )
        val config2 =
            SessionConfig(
                backgroundInactivityTimeout = 10.minutes,
                maxLifetime = 2.hours,
            )

        // Then
        assertEquals(config1.hashCode(), config2.hashCode())
    }

    @Test
    fun `hashCode should be different for different configurations`() {
        // Verifies that different configurations produce different hash codes

        // Given
        val config1 = SessionConfig(backgroundInactivityTimeout = 10.minutes)
        val config2 = SessionConfig(backgroundInactivityTimeout = 15.minutes)

        // Then
        assertNotEquals(config1.hashCode(), config2.hashCode())
    }

    @Test
    fun `withDefaults should always create equal instances`() {
        // Verifies that withDefaults produces consistent, equal instances

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
        // Verifies that data class copy function works as expected for selective updates

        // Given
        val original =
            SessionConfig(
                backgroundInactivityTimeout = 20.minutes,
                maxLifetime = 3.hours,
            )

        // When - copy with modified backgroundInactivityTimeout
        val copied1 = original.copy(backgroundInactivityTimeout = 25.minutes)

        // Then
        assertAll(
            { assertEquals(25.minutes, copied1.backgroundInactivityTimeout) },
            { assertEquals(3.hours, copied1.maxLifetime) }, // unchanged
        )

        // When - copy with modified maxLifetime
        val copied2 = original.copy(maxLifetime = 5.hours)

        // Then
        assertAll(
            { assertEquals(20.minutes, copied2.backgroundInactivityTimeout) }, // unchanged
            { assertEquals(5.hours, copied2.maxLifetime) },
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
        // Verifies that destructuring declarations work correctly for SessionConfig

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
        // Verifies that companion object factory method is accessible both ways

        // When & Then
        assertAll(
            { assertNotNull(SessionConfig.Companion.withDefaults()) },
            { assertNotNull(SessionConfig.withDefaults()) },
            { assertEquals(SessionConfig.Companion.withDefaults(), SessionConfig.withDefaults()) },
        )
    }

    @Test
    fun `should handle edge case durations`() {
        // Verifies that both very small and very large duration values are handled correctly

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
