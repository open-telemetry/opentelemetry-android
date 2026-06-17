/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.common.internal.SemconvCompat.Companion.map
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.context.Context
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

/**
 * This class appends the screen name to all spans.
 */
internal class ScreenAttributesSpanProcessor(
    private val visibleScreenTracker: VisibleScreenTracker,
) : SpanProcessor {
    @OptIn(IncubatingApi::class)
    override fun onStart(
        parentContext: Context,
        span: ReadWriteSpan,
    ) {
        val currentScreen = visibleScreenTracker.currentlyVisibleScreen
        span.setAttribute(stringKey(map(APP_SCREEN_NAME)), currentScreen)
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {
        // nop
    }

    override fun isEndRequired(): Boolean = false
}
