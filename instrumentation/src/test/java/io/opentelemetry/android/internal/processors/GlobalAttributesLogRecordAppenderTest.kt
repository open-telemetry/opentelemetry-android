/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import org.junit.jupiter.api.Test
import java.util.function.Supplier

class GlobalAttributesLogRecordAppenderTest {
    @Test
    fun `Add attributes to every logRecord`() {
        val attributes = Attributes.builder().put("oneAttr", "oneAttrValue").build()
        val attrsSupplier = Supplier { attributes }
        val appender = GlobalAttributesLogRecordAppender(attrsSupplier)
        val log = createLogRecord()

        appender.onEmit(Context.root(), log)

        verify {
            log.setAllAttributes(attributes)
        }
    }

    @Test
    fun `Add updated supplied attrs`() {
        val attributes = Attributes.builder().put("oneAttr", "oneAttrValue").build()
        val attributes2 = Attributes.builder().put("otherAttr", "otherAttrValue").build()
        val attrsSupplier = mockk<Supplier<Attributes>>()
        val appender = GlobalAttributesLogRecordAppender(attrsSupplier)

        // Check first response
        val log = createLogRecord()
        every { attrsSupplier.get() }.returns(attributes)
        appender.onEmit(Context.root(), log)

        verify {
            log.setAllAttributes(attributes)
        }

        // Check second response
        val log2 = createLogRecord()
        every { attrsSupplier.get() }.returns(attributes2)
        appender.onEmit(Context.root(), log2)

        verify {
            log2.setAllAttributes(attributes2)
        }
    }

    private fun createLogRecord(): ReadWriteLogRecord {
        val log = mockk<ReadWriteLogRecord>()
        every { log.setAllAttributes(any()) }.returns(log)
        return log
    }
}
