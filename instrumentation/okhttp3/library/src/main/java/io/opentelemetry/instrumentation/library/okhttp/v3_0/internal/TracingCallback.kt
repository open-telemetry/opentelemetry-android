/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal

import io.opentelemetry.context.Context
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
internal class TracingCallback(
    private val delegate: Callback,
    private val callingContext: Context
) : Callback {

    override fun onFailure(call: Call, e: IOException) {
        callingContext.makeCurrent().use {
            delegate.onFailure(call, e)
        }
    }

    @Throws(IOException::class)
    override fun onResponse(call: Call, response: Response) {
        callingContext.makeCurrent().use {
            delegate.onResponse(call, response)
        }
    }
}
