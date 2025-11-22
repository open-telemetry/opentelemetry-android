/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

/**
 * Tests the functionality of the [SessionIdentifierFacade].
 *
 * Validates that the facade correctly delegates to SessionProvider and properly
 * constructs SessionIdentifiers with both current and previous session IDs.
 */
class SessionIdentifierFacadeTest {
    private val sessionProvider: SessionProvider = mock()

    @Test
    fun `sessionIdentifiers should retrieve current and previous IDs from provider`() {
        // Verifies that facade correctly retrieves both session IDs from the provider

        // Given
        val currentId = UUID.randomUUID().toString()
        val previousId = UUID.randomUUID().toString()
        `when`(sessionProvider.getSessionId()).thenReturn(currentId)
        `when`(sessionProvider.getPreviousSessionId()).thenReturn(previousId)
        val facade = SessionIdentifierFacade(sessionProvider)

        // When
        val identifiers = facade.sessionIdentifiers

        // Then
        assertAll(
            { assertEquals(currentId, identifiers.currentSessionId) },
            { assertEquals(previousId, identifiers.previousSessionId) },
        )
    }

    @Test
    fun `sessionIdentifiers should handle empty previous session ID`() {
        // Verifies that facade correctly handles empty previous session IDs

        // Given
        val currentId = UUID.randomUUID().toString()
        `when`(sessionProvider.getSessionId()).thenReturn(currentId)
        `when`(sessionProvider.getPreviousSessionId()).thenReturn("")
        val facade = SessionIdentifierFacade(sessionProvider)

        // When
        val identifiers = facade.sessionIdentifiers

        // Then
        assertAll(
            { assertEquals(currentId, identifiers.currentSessionId) },
            { assertEquals("", identifiers.previousSessionId) },
        )
    }

    @Test
    fun `sessionIdentifiers should query provider on each access`() {
        // Verifies that facade queries provider on every access, not caching results

        // Given
        val firstCurrentId = UUID.randomUUID().toString()
        val secondCurrentId = UUID.randomUUID().toString()
        val previousId = UUID.randomUUID().toString()

        `when`(sessionProvider.getSessionId())
            .thenReturn(firstCurrentId)
            .thenReturn(secondCurrentId)
        `when`(sessionProvider.getPreviousSessionId())
            .thenReturn("")
            .thenReturn(previousId)

        val facade = SessionIdentifierFacade(sessionProvider)

        // When - first access
        val firstIdentifiers = facade.sessionIdentifiers

        // Then
        assertAll(
            { assertEquals(firstCurrentId, firstIdentifiers.currentSessionId) },
            { assertEquals("", firstIdentifiers.previousSessionId) },
        )

        // When - second access (simulating session transition)
        val secondIdentifiers = facade.sessionIdentifiers

        // Then
        assertAll(
            { assertEquals(secondCurrentId, secondIdentifiers.currentSessionId) },
            { assertEquals(previousId, secondIdentifiers.previousSessionId) },
            { verify(sessionProvider, times(2)).getSessionId() },
            { verify(sessionProvider, times(2)).getPreviousSessionId() },
        )
    }

    @Test
    fun `sessionIdentifiers should work with noop SessionProvider`() {
        // Verifies that facade works correctly with no-op provider

        // Given
        val facade = SessionIdentifierFacade(SessionProvider.getNoop())

        // When
        val identifiers = facade.sessionIdentifiers

        // Then
        assertAll(
            { assertEquals("", identifiers.currentSessionId) },
            { assertEquals("", identifiers.previousSessionId) },
        )
    }

    @Test
    fun `sessionIdentifiers should handle multiple rapid accesses`() {
        // Verifies that facade handles repeated rapid accesses without errors

        // Given
        val currentId = UUID.randomUUID().toString()
        val previousId = UUID.randomUUID().toString()
        `when`(sessionProvider.getSessionId()).thenReturn(currentId)
        `when`(sessionProvider.getPreviousSessionId()).thenReturn(previousId)
        val facade = SessionIdentifierFacade(sessionProvider)

        // When - access multiple times rapidly
        val results = (1..10).map { facade.sessionIdentifiers }

        // Then - all should return consistent results
        results.forEach { identifiers ->
            assertAll(
                { assertEquals(currentId, identifiers.currentSessionId) },
                { assertEquals(previousId, identifiers.previousSessionId) },
            )
        }

        // Verify provider was called for each access
        assertAll(
            { verify(sessionProvider, times(10)).getSessionId() },
            { verify(sessionProvider, times(10)).getPreviousSessionId() },
        )
    }

    @Test
    fun `sessionIdentifiers should reflect session transitions from provider`() {
        // Verifies that facade reflects all session transitions as they occur

        // Given
        val session1 = UUID.randomUUID().toString()
        val session2 = UUID.randomUUID().toString()
        val session3 = UUID.randomUUID().toString()

        // Simulate session lifecycle: session1 -> session2 -> session3
        `when`(sessionProvider.getSessionId())
            .thenReturn(session1) // Initial session
            .thenReturn(session2) // First transition
            .thenReturn(session3) // Second transition

        `when`(sessionProvider.getPreviousSessionId())
            .thenReturn("") // No previous initially
            .thenReturn(session1) // session1 is now previous
            .thenReturn(session2) // session2 is now previous

        val facade = SessionIdentifierFacade(sessionProvider)

        // When/Then - Initial state
        val initial = facade.sessionIdentifiers
        assertAll(
            { assertEquals(session1, initial.currentSessionId) },
            { assertEquals("", initial.previousSessionId) },
        )

        // When/Then - First transition
        val afterFirst = facade.sessionIdentifiers
        assertAll(
            { assertEquals(session2, afterFirst.currentSessionId) },
            { assertEquals(session1, afterFirst.previousSessionId) },
        )

        // When/Then - Second transition
        val afterSecond = facade.sessionIdentifiers
        assertAll(
            { assertEquals(session3, afterSecond.currentSessionId) },
            { assertEquals(session2, afterSecond.previousSessionId) },
        )
    }
}
