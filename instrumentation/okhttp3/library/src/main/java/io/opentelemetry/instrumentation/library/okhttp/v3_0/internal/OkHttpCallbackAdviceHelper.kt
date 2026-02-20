/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal

import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.util.VirtualField
import okhttp3.Call
import okhttp3.Request

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
internal object OkHttpCallbackAdviceHelper {

    @JvmStatic
    fun propagateContext(call: Call): Boolean {
        val context = Context.current()
        if (shouldPropagateContext(context)) {
            val virtualField = VirtualField.find(
                Request::class.java,
                Context::class.java
            )
            virtualField.set(call.request(), context)
            return true
        }
        return false
    }

    @JvmStatic
    fun tryRecoverPropagatedContextFromCallback(request: Request): Context? {
        val virtualField = VirtualField.find(
            Request::class.java,
            Context::class.java
        )
        return virtualField.get(request)
    }

    private fun shouldPropagateContext(context: Context): Boolean = context != Context.root()
}
