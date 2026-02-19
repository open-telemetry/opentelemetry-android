/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection.internal

import io.opentelemetry.context.propagation.TextMapSetter
import java.net.URLConnection

internal object RequestPropertySetter : TextMapSetter<URLConnection?> {
    override fun set(
        carrier: URLConnection?,
        key: String,
        value: String,
    ) {
        carrier?.setRequestProperty(key, value)
    }
}
