/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.instrumentation

import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import java.util.ServiceLoader

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
class AndroidInstrumentationLoaderImpl : AndroidInstrumentationLoader {
    private val instrumentations: MutableMap<Class<out AndroidInstrumentation>, AndroidInstrumentation> by lazy {
        ServiceLoader.load(AndroidInstrumentation::class.java).associateBy { it.javaClass }
            .toMutableMap()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : AndroidInstrumentation> getByType(type: Class<out T>): T? {
        return instrumentations[type] as? T
    }

    override fun getAll(): Collection<AndroidInstrumentation> {
        return instrumentations.values.toList()
    }

    @Throws(IllegalStateException::class)
    fun registerForTest(instrumentation: AndroidInstrumentation) {
        if (instrumentation::class.java in instrumentations) {
            throw IllegalStateException("Instrumentation with type '${instrumentation::class.java}' already exists.")
        }
        instrumentations[instrumentation.javaClass] = instrumentation
    }
}
