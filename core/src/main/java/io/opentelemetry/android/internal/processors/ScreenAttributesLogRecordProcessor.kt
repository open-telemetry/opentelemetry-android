/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.processors

import io.opentelemetry.android.common.internal.SemconvCompat.Companion.map
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.context.Context
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

class ScreenAttributesLogRecordProcessor(
    val visibleScreenTracker: VisibleScreenTracker,
) : LogRecordProcessor {
    @OptIn(IncubatingApi::class)
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        val currentScreen = visibleScreenTracker.currentlyVisibleScreen
        logRecord.setAttribute(stringKey(map(APP_SCREEN_NAME)), currentScreen)
    }
}
