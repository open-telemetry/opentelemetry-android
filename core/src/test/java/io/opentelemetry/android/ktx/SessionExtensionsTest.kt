/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.ktx

import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * Validates session extension functions for setting session identifiers on telemetry data.
 */
private const val CURRENT_ID = "current-session-123"
private const val PREVIOUS_ID = "previous-session-456"
private const val EMPTY_PREVIOUS_ID = ""
private const val BLANK_PREVIOUS_ID = "   "

class SessionExtensionsTest {
    @Test
    fun `setSessionIdentifiersWith sets current and previous when previous non-empty for Span`() {
        // Verifies that extension function adds both session IDs to Span when previous exists

        // Given
        val span = mockk<Span>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = span.setSessionIdentifiersWith(provider)

        // Then
        assertSame(span, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous empty for Span`() {
        // Verifies that only current session ID is added when previous is empty

        // Given
        val span = mockk<Span>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns EMPTY_PREVIOUS_ID

        // When
        val result = span.setSessionIdentifiersWith(provider)

        // Then
        assertSame(span, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous blank for Span`() {
        // Verifies that blank previous session IDs are treated as empty

        // Given
        val span = mockk<Span>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns BLANK_PREVIOUS_ID

        // When
        val result = span.setSessionIdentifiersWith(provider)

        // Then
        assertSame(span, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets current and previous when previous non-empty for LogRecordBuilder`() {
        // Verifies that extension function adds both session IDs to LogRecordBuilder when previous exists

        // Given
        val builder = mockk<LogRecordBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous empty for LogRecordBuilder`() {
        // Verifies that only current session ID is added to LogRecordBuilder when previous is empty

        // Given
        val builder = mockk<LogRecordBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns EMPTY_PREVIOUS_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous blank for LogRecordBuilder`() {
        // Verifies that blank previous session IDs are treated as empty for LogRecordBuilder

        // Given
        val builder = mockk<LogRecordBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns BLANK_PREVIOUS_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets current and previous when previous non-empty for AttributesBuilder`() {
        // Verifies that extension function adds both session IDs to AttributesBuilder when previous exists

        // Given
        val builder = mockk<AttributesBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous empty for AttributesBuilder`() {
        // Verifies that only current session ID is added to AttributesBuilder when previous is empty

        // Given
        val builder = mockk<AttributesBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns EMPTY_PREVIOUS_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous blank for AttributesBuilder`() {
        // Verifies that blank previous session IDs are treated as empty for AttributesBuilder

        // Given
        val builder = mockk<AttributesBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns BLANK_PREVIOUS_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets current and previous when previous non-empty for ReadWriteLogRecord`() {
        // Verifies that extension function adds both session IDs to ReadWriteLogRecord when previous exists

        // Given
        val record = mockk<ReadWriteLogRecord>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = record.setSessionIdentifiersWith(provider)

        // Then
        assertSame(record, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous empty for ReadWriteLogRecord`() {
        // Verifies that only current session ID is added to ReadWriteLogRecord when previous is empty

        // Given
        val record = mockk<ReadWriteLogRecord>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns EMPTY_PREVIOUS_ID

        // When
        val result = record.setSessionIdentifiersWith(provider)

        // Then
        assertSame(record, result)
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous blank for ReadWriteLogRecord`() {
        // Verifies that blank previous session IDs are treated as empty for ReadWriteLogRecord

        // Given
        val record = mockk<ReadWriteLogRecord>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns BLANK_PREVIOUS_ID

        // When
        val result = record.setSessionIdentifiersWith(provider)

        // Then
        assertSame(record, result)
    }
}
