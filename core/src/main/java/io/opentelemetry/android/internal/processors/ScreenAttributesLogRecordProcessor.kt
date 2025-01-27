/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

class ScreenAttributesLogRecordProcessor(
    val visibleScreenTracker: VisibleScreenTracker,
) : LogRecordProcessor {
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        val currentScreen = visibleScreenTracker.currentlyVisibleScreen
        logRecord.setAttribute(RumConstants.SCREEN_NAME_KEY, currentScreen)
    }
}
