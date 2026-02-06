/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import java.util.ServiceLoader

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
internal class AndroidInstrumentationLoaderImpl : AndroidInstrumentationLoader {
    private val instrumentations: MutableMap<Class<out AndroidInstrumentation>, AndroidInstrumentation> by lazy {
        ServiceLoader
            .load(AndroidInstrumentation::class.java)
            .associateBy { it.javaClass }
            .toMutableMap()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : AndroidInstrumentation> getByType(type: Class<out T>): T =
        instrumentations[type] as? T ?: error("Instrumentation not found for $type")

    override fun getAll(): Collection<AndroidInstrumentation> = instrumentations.values.toList()

    @Throws(IllegalStateException::class)
    fun registerForTest(instrumentation: AndroidInstrumentation) {
        check(instrumentation::class.java !in instrumentations) {
            "Instrumentation with type '${instrumentation::class.java}' already exists."
        }
        instrumentations[instrumentation.javaClass] = instrumentation
    }
}
