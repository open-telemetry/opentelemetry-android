/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

private const val DEFAULT_SESSION_ID = "42"
private const val CURRENT_SESSION_ID = "current-session-123"
private const val PREVIOUS_SESSION_ID = "previous-session-456"

/**
 * Verifies [SessionIdSpanAppender] correctly injects session identifiers into spans,
 * including both current and previous session IDs when available.
 */
internal class SessionIdSpanAppenderTest {
    @MockK
    lateinit var sessionProvider: SessionProvider

    @MockK
    lateinit var span: ReadWriteSpan

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { sessionProvider.getSessionId() }.returns(DEFAULT_SESSION_ID)
        every { sessionProvider.getPreviousSessionId() }.returns("")
        every { span.setAttribute(any<AttributeKey<String>>(), any<String>()) } returns span
    }

    @Test
    fun `should set sessionId as span attribute`() {
        // Verifies that session ID is added to spans as an attribute

        // Given
        val underTest = SessionIdSpanAppender(sessionProvider)

        // When/Then
        assertTrue(underTest.isStartRequired)
        underTest.onStart(Context.root(), span)

        // Then
        verify { span.setAttribute(SessionIncubatingAttributes.SESSION_ID, DEFAULT_SESSION_ID) }

        assertFalse(underTest.isEndRequired)
    }

    @Test
    fun `should set both session IDs when previous session exists`() {
        // Verifies that both current and previous session IDs are added when available

        // Given
        every { sessionProvider.getSessionId() }.returns(CURRENT_SESSION_ID)
        every { sessionProvider.getPreviousSessionId() }.returns(PREVIOUS_SESSION_ID)
        val underTest = SessionIdSpanAppender(sessionProvider)

        // When
        underTest.onStart(Context.root(), span)

        // Then
        assertAll(
            { verify { span.setAttribute(SessionIncubatingAttributes.SESSION_ID, CURRENT_SESSION_ID) } },
            { verify { span.setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, PREVIOUS_SESSION_ID) } },
        )
    }

    @Test
    fun `should not set previous session ID when empty`() {
        // Verifies that previous session ID is omitted when not available

        // Given
        every { sessionProvider.getSessionId() }.returns(CURRENT_SESSION_ID)
        every { sessionProvider.getPreviousSessionId() }.returns("")
        val underTest = SessionIdSpanAppender(sessionProvider)

        // When
        underTest.onStart(Context.root(), span)

        // Then
        assertAll(
            { verify { span.setAttribute(SessionIncubatingAttributes.SESSION_ID, CURRENT_SESSION_ID) } },
            { verify(exactly = 0) { span.setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, any()) } },
        )
    }
}
