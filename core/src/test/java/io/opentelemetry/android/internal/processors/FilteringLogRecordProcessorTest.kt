/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class FilteringLogRecordProcessorTest {
    @Test
    fun `forwards only accepted records with their original context`() {
        val delegate = mockk<LogRecordProcessor>(relaxed = true)
        val accepted = mockk<ReadWriteLogRecord>()
        val rejected = mockk<ReadWriteLogRecord>()
        val context = Context.root()
        val filter = FilteringLogRecordProcessor(delegate) { it === accepted }
        filter.onEmit(context, rejected)
        filter.onEmit(context, accepted)
        verify(exactly = 0) { delegate.onEmit(any(), rejected) }
        verify(exactly = 1) { delegate.onEmit(context, accepted) }
    }

    @Test
    fun `forwards flush shutdown and close including incomplete and failed results`() {
        val delegate = mockk<LogRecordProcessor>(relaxed = true)
        val pending = CompletableResultCode()
        val failure = CompletableResultCode.ofFailure()
        every { delegate.forceFlush() } returns pending
        every { delegate.shutdown() } returns failure
        val filter = FilteringLogRecordProcessor(delegate) { false }
        assertThat(filter.forceFlush()).isSameAs(pending)
        assertThat(filter.shutdown()).isSameAs(failure)
        filter.close()
        verify(exactly = 1) { delegate.close() }
    }

    @Test
    fun `does not invent a fallback decision when the predicate throws`() {
        val delegate = mockk<LogRecordProcessor>(relaxed = true)
        val failure = IllegalStateException("lookup unavailable")
        val filter = FilteringLogRecordProcessor(delegate) { throw failure }
        assertThatThrownBy { filter.onEmit(Context.root(), mockk()) }.isSameAs(failure)
        verify(exactly = 0) { delegate.onEmit(any(), any()) }
    }
}
