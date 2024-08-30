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

internal class SessionIdSpanAppenderTest {
    @MockK
    lateinit var sessionProvider: SessionProvider

    @MockK
    lateinit var span: ReadWriteSpan

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { sessionProvider.getSessionId() }.returns("42")
        every { span.setAttribute(any<AttributeKey<String>>(), any<String>()) } returns span
    }

    @Test
    fun `should set sessionId as span attribute`() {
        val underTest = SessionIdSpanAppender(sessionProvider)

        assertTrue(underTest.isStartRequired)
        underTest.onStart(Context.root(), span)

        verify { span.setAttribute(SessionIncubatingAttributes.SESSION_ID, "42") }

        assertFalse(underTest.isEndRequired)
    }
}
