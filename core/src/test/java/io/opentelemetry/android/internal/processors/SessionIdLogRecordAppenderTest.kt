/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

private const val SESSION_ID_VALUE = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
private const val PREVIOUS_SESSION_ID_VALUE = "f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1"

/**
 * Validates [SessionIdLogRecordAppender] correctly injects session identifiers into log records,
 * including both current and previous session IDs when available.
 */
class SessionIdLogRecordAppenderTest {
    @MockK
    lateinit var sessionProvider: SessionProvider

    @MockK
    lateinit var log: ReadWriteLogRecord

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { sessionProvider.getSessionId() }.returns(SESSION_ID_VALUE)
        every { sessionProvider.getPreviousSessionId() }.returns(SessionProvider.NO_SESSION_ID)
        every { log.setAttribute(any<AttributeKey<String>>(), any<String>()) } returns log
    }

    @Test
    fun `should set sessionId as log record attribute`() {
        // Given
        val underTest = SessionIdLogRecordAppender(sessionProvider)

        // When
        underTest.onEmit(Context.root(), log)

        // Then
        verify { log.setAttribute(SessionIncubatingAttributes.SESSION_ID, SESSION_ID_VALUE) }
    }

    @Test
    fun `should set both session IDs when previous session exists`() {
        // Given
        every { sessionProvider.getSessionId() }.returns(SESSION_ID_VALUE)
        every { sessionProvider.getPreviousSessionId() }.returns(PREVIOUS_SESSION_ID_VALUE)
        val underTest = SessionIdLogRecordAppender(sessionProvider)

        // When
        underTest.onEmit(Context.root(), log)

        // Then
        assertAll(
            { verify { log.setAttribute(SessionIncubatingAttributes.SESSION_ID, SESSION_ID_VALUE) } },
            { verify { log.setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, PREVIOUS_SESSION_ID_VALUE) } },
        )
    }

    @Test
    fun `should not set previous session ID when empty`() {
        // Given
        every { sessionProvider.getSessionId() }.returns(SESSION_ID_VALUE)
        every { sessionProvider.getPreviousSessionId() }.returns(SessionProvider.NO_SESSION_ID)
        val underTest = SessionIdLogRecordAppender(sessionProvider)

        // When
        underTest.onEmit(Context.root(), log)

        // Then
        assertAll(
            { verify { log.setAttribute(SessionIncubatingAttributes.SESSION_ID, SESSION_ID_VALUE) } },
            { verify(exactly = 0) { log.setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, any()) } },
        )
    }
}
