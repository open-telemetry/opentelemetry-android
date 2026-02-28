/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.ktx

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

private const val CURRENT_ID = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
private const val PREVIOUS_ID = "f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1"

/**
 * Validates session extension functions for setting session identifiers on telemetry data.
 */
@OptIn(Incubating::class)
class SessionExtensionsTest {
    @Test
    fun `setSessionIdentifiersWith sets current and previous when previous non-empty for Span`() {
        // Given
        val span = mockk<Span>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = span.setSessionIdentifiersWith(provider)

        // Then
        assertSame(span, result)
        verify { span.setAttribute(SessionIncubatingAttributes.SESSION_ID, CURRENT_ID) }
        verify { span.setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, PREVIOUS_ID) }
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous empty for Span`() {
        // Given
        val span = mockk<Span>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns SessionProvider.NO_SESSION_ID

        // When
        val result = span.setSessionIdentifiersWith(provider)

        // Then
        assertSame(span, result)
        verify { span.setAttribute(SessionIncubatingAttributes.SESSION_ID, CURRENT_ID) }
        verify(exactly = 0) { span.setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, any()) }
    }

    @Test
    fun `setSessionIdentifiersWith skips attributes when current session ID is empty for Span`() {
        // Given
        val span = mockk<Span>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns SessionProvider.NO_SESSION_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = span.setSessionIdentifiersWith(provider)

        // Then
        assertSame(span, result)
        verify(exactly = 0) { span.setAttribute(any<AttributeKey<String>>(), any()) }
    }

    @Test
    fun `setSessionIdentifiersWith sets current and previous when previous non-empty for LogRecordBuilder`() {
        // Given
        val builder = mockk<LogRecordBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
        verify { builder.setAttribute(SessionIncubatingAttributes.SESSION_ID, CURRENT_ID) }
        verify { builder.setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, PREVIOUS_ID) }
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous empty for LogRecordBuilder`() {
        // Given
        val builder = mockk<LogRecordBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns SessionProvider.NO_SESSION_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
        verify { builder.setAttribute(SessionIncubatingAttributes.SESSION_ID, CURRENT_ID) }
        verify(exactly = 0) { builder.setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, any()) }
    }

    @Test
    fun `setSessionIdentifiersWith skips attributes when current session ID is empty for LogRecordBuilder`() {
        // Given
        val builder = mockk<LogRecordBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns SessionProvider.NO_SESSION_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
        verify(exactly = 0) { builder.setAttribute(any<AttributeKey<String>>(), any()) }
    }

    @Test
    fun `setSessionIdentifiersWith sets current and previous when previous non-empty for AttributesBuilder`() {
        // Given
        val builder = mockk<AttributesBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
        verify { builder.put(SessionIncubatingAttributes.SESSION_ID, CURRENT_ID) }
        verify { builder.put(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, PREVIOUS_ID) }
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous empty for AttributesBuilder`() {
        // Given
        val builder = mockk<AttributesBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns SessionProvider.NO_SESSION_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
        verify { builder.put(SessionIncubatingAttributes.SESSION_ID, CURRENT_ID) }
        verify(exactly = 0) { builder.put(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, any()) }
    }

    @Test
    fun `setSessionIdentifiersWith skips attributes when current session ID is empty for AttributesBuilder`() {
        // Given
        val builder = mockk<AttributesBuilder>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns SessionProvider.NO_SESSION_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = builder.setSessionIdentifiersWith(provider)

        // Then
        assertSame(builder, result)
        verify(exactly = 0) { builder.put(any<AttributeKey<String>>(), any<String>()) }
    }

    @Test
    fun `setSessionIdentifiersWith sets current and previous when previous non-empty for ReadWriteLogRecord`() {
        // Given
        val record = mockk<ReadWriteLogRecord>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = record.setSessionIdentifiersWith(provider)

        // Then
        assertSame(record, result)
        verify { record.setAttribute(SessionIncubatingAttributes.SESSION_ID, CURRENT_ID) }
        verify { record.setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, PREVIOUS_ID) }
    }

    @Test
    fun `setSessionIdentifiersWith sets only current when previous empty for ReadWriteLogRecord`() {
        // Given
        val record = mockk<ReadWriteLogRecord>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns CURRENT_ID
        every { provider.getPreviousSessionId() } returns SessionProvider.NO_SESSION_ID

        // When
        val result = record.setSessionIdentifiersWith(provider)

        // Then
        assertSame(record, result)
        verify { record.setAttribute(SessionIncubatingAttributes.SESSION_ID, CURRENT_ID) }
        verify(exactly = 0) { record.setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, any()) }
    }

    @Test
    fun `setSessionIdentifiersWith skips attributes when current session ID is empty for ReadWriteLogRecord`() {
        // Given
        val record = mockk<ReadWriteLogRecord>(relaxed = true)
        val provider = mockk<SessionProvider>()
        every { provider.getSessionId() } returns SessionProvider.NO_SESSION_ID
        every { provider.getPreviousSessionId() } returns PREVIOUS_ID

        // When
        val result = record.setSessionIdentifiersWith(provider)

        // Then
        assertSame(record, result)
        verify(exactly = 0) { record.setAttribute(any<AttributeKey<String>>(), any()) }
    }
}
