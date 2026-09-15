/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

import java.util.ServiceLoader

/**
 * Discovers [ApiExtension] implementations declared via SPI (`@AutoService(ApiExtension::class)`).
 */
internal object ApiExtensionLoader {
    /**
     * See the R8 note on
     * [io.opentelemetry.android.instrumentation.AndroidInstrumentationLoaderImpl.loadInstrumentations]
     * before changing the shape of this function. The two-argument `load` overload, the literal
     * class reference and the local for-each are required for R8 to replace the reflective lookup
     * with direct instantiation.
     */
    fun load(): List<ApiExtension> {
        val extensions = mutableListOf<ApiExtension>()
        for (extension in ServiceLoader.load(ApiExtension::class.java, ApiExtension::class.java.classLoader)) {
            extensions.add(extension)
        }
        return extensions
    }
}
