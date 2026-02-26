/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common

import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope

class ActiveSpan(
    private val lastVisibleScreen: () -> String?,
) {
    private var span: Span? = null
    private var scope: Scope? = null

    fun spanInProgress(): Boolean = span != null

    // it's fine to not close the scope here, will be closed in endActiveSpan()
    fun startSpan(spanCreator: () -> Span) {
        // don't start one if there's already one in progress
        if (span != null) {
            return
        }
        span = spanCreator()
        scope = span?.makeCurrent()
    }

    fun endActiveSpan() {
        scope?.let {
            it.close()
            scope = null
        }
        span?.let {
            it.end()
            span = null
        }
    }

    fun addEvent(eventName: String) {
        span?.addEvent(eventName)
    }

    fun addPreviousScreenAttribute(screenName: String) {
        span?.let {
            val previouslyVisibleScreen = lastVisibleScreen()
            if (previouslyVisibleScreen != null && screenName != previouslyVisibleScreen) {
                it.setAttribute(RumConstants.LAST_SCREEN_NAME_KEY, previouslyVisibleScreen)
            }
        }
    }
}
