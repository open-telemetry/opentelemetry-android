/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import org.junit.Test

private const val CURRENT_SCREEN = "party favors"

class ScreenAttributesLogRecordProcessorTest {
    @Test
    fun `current screen name is appended`() {
        val visibleScreenService: VisibleScreenService = mockk()
        val logRecord: ReadWriteLogRecord = mockk()
        every { visibleScreenService.currentlyVisibleScreen }.returns(CURRENT_SCREEN)
        every { logRecord.setAttribute(any<AttributeKey<String>>(), any<String>()) } returns logRecord
        val testClass = ScreenAttributesLogRecordProcessor(visibleScreenService)
        testClass.onEmit(mockk(), logRecord)
        verify { logRecord.setAttribute(SCREEN_NAME_KEY, CURRENT_SCREEN) }
    }
}
