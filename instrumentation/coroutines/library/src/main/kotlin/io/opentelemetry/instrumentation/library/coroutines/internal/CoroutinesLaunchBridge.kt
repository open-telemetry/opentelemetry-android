/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.coroutines.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.functions.Function2

/**
 * Intercepts `kotlinx.coroutines.BuildersKt.launch` and `BuildersKt.launch$default` calls that
 * the ByteBuddy agent plugin rewrites in application classes.
 *
 * These methods must be public JVM methods with descriptors identical to the originals because the
 * plugin changes only the call owner in the rewritten bytecode.
 */
object CoroutinesLaunchBridge {
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun launch(
        scope: CoroutineScope,
        context: CoroutineContext,
        start: CoroutineStart,
        block: Function2<*, *, *>,
    ): Job {
        val enriched = CoroutinesContextHelper.addCurrentContextIfNeeded(context)
        return scope.launch(enriched, start, block as (suspend CoroutineScope.() -> Unit))
    }

    @JvmStatic
    @JvmName("launch\$default")
    @Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER")
    fun launchWithDefaults(
        scope: CoroutineScope,
        context: CoroutineContext?,
        start: CoroutineStart?,
        block: Function2<*, *, *>,
        mask: Int,
        handler: Any?,
    ): Job {
        val actualContext =
            if (mask and 0x1 != 0) {
                // The context param has been omitted
                EmptyCoroutineContext
            } else {
                context ?: EmptyCoroutineContext
            }

        val actualStart =
            if (mask and 0x2 != 0) {
                // The start param has been omitted
                CoroutineStart.DEFAULT
            } else {
                start ?: CoroutineStart.DEFAULT
            }

        val enriched = CoroutinesContextHelper.addCurrentContextIfNeeded(actualContext)
        return scope.launch(enriched, actualStart, block as (suspend CoroutineScope.() -> Unit))
    }
}
