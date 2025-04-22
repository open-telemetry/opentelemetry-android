/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

/**
 * This class appends the screen name to all spans.
 */
internal class ScreenAttributesSpanProcessor(
    private val visibleScreenTracker: VisibleScreenTracker,
) : SpanProcessor {
    override fun onStart(
        parentContext: Context,
        span: ReadWriteSpan,
    ) {
        val currentScreen = visibleScreenTracker.currentlyVisibleScreen
        span.setAttribute(RumConstants.SCREEN_NAME_KEY, currentScreen)
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {
        // nop
    }

    override fun isEndRequired(): Boolean = false
}
