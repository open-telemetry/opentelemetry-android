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

private const val SESSION_ID_VALUE = "0666"

class SessionIdLogRecordAppenderTest {
    @MockK
    lateinit var sessionProvider: SessionProvider

    @MockK
    lateinit var log: ReadWriteLogRecord

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { sessionProvider.getSessionId() }.returns(SESSION_ID_VALUE)
        every { log.setAttribute(any<AttributeKey<String>>(), any<String>()) } returns log
    }

    @Test
    fun `should set sessionId as log record attribute`() {
        val underTest = SessionIdLogRecordAppender(sessionProvider)

        underTest.onEmit(Context.root(), log)

        verify { log.setAttribute(SessionIncubatingAttributes.SESSION_ID, SESSION_ID_VALUE) }
    }
}
