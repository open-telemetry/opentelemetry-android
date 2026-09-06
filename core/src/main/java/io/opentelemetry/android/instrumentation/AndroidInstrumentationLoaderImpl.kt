/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import android.util.Log
import io.opentelemetry.android.common.RumConstants.OTEL_RUM_LOG_TAG
import java.util.ServiceLoader

internal class AndroidInstrumentationLoaderImpl : AndroidInstrumentationLoader {
    private val instrumentations: MutableMap<Class<out AndroidInstrumentation>, AndroidInstrumentation> by lazy {
        loadInstrumentations()
            .associateBy { it.javaClass }
            .toMutableMap()
    }

    /**
     * Loads the [AndroidInstrumentation] implementations declared via SPI. Before making changes to
     * this function please study R8's ServiceLoaderRewriter as it replaces
     * reflective lookup with direct instantiation, keeping reflection off the startup path. The
     * two-argument `load` overload, the literal class reference and the local for-each are all
     * required for this optimization to occur.
     *
     * https://r8.googlesource.com/r8/+/refs/heads/main/src/main/java/com/android/tools/r8/ir/optimize/ServiceLoaderRewriter.java
     */
    private fun loadInstrumentations(): List<AndroidInstrumentation> {
        val instrumentations = mutableListOf<AndroidInstrumentation>()
        for (instrumentation in ServiceLoader.load(AndroidInstrumentation::class.java, AndroidInstrumentation::class.java.classLoader)) {
            instrumentations.add(instrumentation)
        }
        return instrumentations
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : AndroidInstrumentation> getByType(type: Class<out T>): T? {
        val result = instrumentations[type] as? T
        if (result == null) {
            Log.w(OTEL_RUM_LOG_TAG, "Instrumentation not found for $type")
        }
        return result
    }

    override fun getAll(): Collection<AndroidInstrumentation> = instrumentations.values.toList()

    @Throws(IllegalStateException::class)
    fun registerForTest(instrumentation: AndroidInstrumentation) {
        check(instrumentation::class.java !in instrumentations) {
            "Instrumentation with type '${instrumentation::class.java}' already exists."
        }
        instrumentations[instrumentation.javaClass] = instrumentation
    }
}
