/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

import java.util.ServiceLoader

internal object ApiExtensionLoader {
    /**
     * Keep the two-argument load, literal service class and local iteration together for R8's
     * ServiceLoader rewriting.
     */
    fun load(): List<ApiExtension> {
        val extensions = mutableListOf<ApiExtension>()
        for (extension in ServiceLoader.load(ApiExtension::class.java, ApiExtension::class.java.classLoader)) {
            extensions.add(extension)
        }
        return extensions
    }
}
