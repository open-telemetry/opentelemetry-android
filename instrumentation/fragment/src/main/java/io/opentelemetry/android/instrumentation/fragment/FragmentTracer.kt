/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment

import androidx.fragment.app.Fragment
import io.opentelemetry.android.semconv.AppAttributes.APP_SCREEN_PREVIOUS_NAME
import io.opentelemetry.android.semconv.FragmentAttributes.FRAGMENT_NAME_KEY
import io.opentelemetry.android.semconv.internal.SemconvCompat.Companion.map
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi

internal class FragmentTracer(
    fragment: Fragment,
    private val tracer: Tracer,
    private val screenName: String,
    private val lastVisibleScreen: () -> String?,
) {
    private val fragmentName: String = fragment.javaClass.simpleName
    private var span: Span? = null

    fun startSpanIfNoneInProgress(action: String): FragmentTracer {
        if (span != null) {
            return this
        }
        span = createSpan(action)
        return this
    }

    fun startFragmentCreation(): FragmentTracer {
        if (span != null) {
            return this
        }
        span = createSpan("Created")
        return this
    }

    @OptIn(IncubatingApi::class)
    private fun createSpan(spanName: String): Span {
        val span =
            tracer
                .spanBuilder(spanName)
                .setAttribute(FRAGMENT_NAME_KEY, fragmentName)
                .startSpan()
        // do this after the span is started, so we can override the default app.screen.name set by the
        // RumAttributeAppender.
        span.setAttribute(map(APP_SCREEN_NAME), screenName)
        return span
    }

    fun endActiveSpan() {
        span?.end()
        span = null
    }

    fun addPreviousScreenAttribute(): FragmentTracer {
        val previouslyVisibleScreen = lastVisibleScreen()
        if (previouslyVisibleScreen != null && fragmentName != previouslyVisibleScreen) {
            span?.setAttribute(map(APP_SCREEN_PREVIOUS_NAME), previouslyVisibleScreen)
        }
        return this
    }

    fun addEvent(eventName: String): FragmentTracer {
        span?.addEvent(eventName)
        return this
    }
}
