/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment

import androidx.fragment.app.Fragment
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.instrumentation.common.ActiveSpan
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer

internal class FragmentTracer(
    fragment: Fragment,
    private val tracer: Tracer,
    private val screenName: String,
    private val activeSpan: ActiveSpan,
) {
    private val fragmentName: String = fragment.javaClass.simpleName

    fun startSpanIfNoneInProgress(action: String): FragmentTracer {
        if (activeSpan.spanInProgress()) {
            return this
        }
        activeSpan.startSpan { createSpan(action) }
        return this
    }

    fun startFragmentCreation(): FragmentTracer {
        activeSpan.startSpan { createSpan("Created") }
        return this
    }

    private fun createSpan(spanName: String): Span {
        val span =
            tracer
                .spanBuilder(spanName)
                .setAttribute(FRAGMENT_NAME_KEY, fragmentName)
                .startSpan()
        // do this after the span is started, so we can override the default screen.name set by the
        // RumAttributeAppender.
        span.setAttribute(RumConstants.SCREEN_NAME_KEY, screenName)
        return span
    }

    fun endActiveSpan() {
        activeSpan.endActiveSpan()
    }

    fun addPreviousScreenAttribute(): FragmentTracer {
        activeSpan.addPreviousScreenAttribute(fragmentName)
        return this
    }

    fun addEvent(eventName: String): FragmentTracer {
        activeSpan.addEvent(eventName)
        return this
    }

    companion object {
        val FRAGMENT_NAME_KEY: AttributeKey<String?> = AttributeKey.stringKey("fragment.name")
    }
}
