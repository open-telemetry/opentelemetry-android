/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Exercises both launch overloads from application bytecode so that ByteBuddy transforms them.
 * These functions must live in src/main so that they are included in the transformed application
 * classes.
 */
object CoroutinesTestUtil {
    fun launchDefault(
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = scope.launch { block() }

    fun launchDirect(
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = scope.launch(EmptyCoroutineContext, CoroutineStart.DEFAULT) { block() }
}
