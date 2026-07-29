/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.coroutines.internal

import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.extension.kotlin.getOpenTelemetryContext
import kotlin.coroutines.CoroutineContext

/**
 * Decides whether the current OpenTelemetry [Context] should be added to an about-to-launch
 * coroutine. Not intended as public API; public JVM visibility is required so that the runtime
 * instrumentation in the parent package can configure it.
 */
object CoroutinesContextHelper {
    @Volatile
    private var enabled = false

    @JvmStatic
    fun setEnabled(value: Boolean) {
        enabled = value
    }

    /**
     * Returns the supplied [coroutineContext] enriched with the current OpenTelemetry context
     * when all of the following are true:
     *
     * - Runtime instrumentation is enabled.
     * - The current OpenTelemetry context is not root.
     * - The coroutine context does not already carry a non-root OpenTelemetry context (user
     *   supplied context takes precedence).
     */
    @JvmStatic
    fun addCurrentContextIfNeeded(coroutineContext: CoroutineContext): CoroutineContext {
        if (!enabled) {
            return coroutineContext
        }

        val current = Context.current()
        if (current === Context.root()) {
            return coroutineContext
        }

        val existing = coroutineContext.getOpenTelemetryContext()
        if (existing !== Context.root()) {
            return coroutineContext
        }

        return coroutineContext + current.asContextElement()
    }
}
