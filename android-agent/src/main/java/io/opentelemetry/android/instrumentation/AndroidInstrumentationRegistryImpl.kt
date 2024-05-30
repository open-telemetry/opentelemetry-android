/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import java.util.Collections
import java.util.ServiceLoader

internal class AndroidInstrumentationRegistryImpl : AndroidInstrumentationRegistry {
    private val instrumentations: MutableMap<Class<out AndroidInstrumentation>, AndroidInstrumentation> by lazy {
        ServiceLoader.load(AndroidInstrumentation::class.java).associateBy { it.javaClass }
            .toMutableMap()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : AndroidInstrumentation> get(type: Class<out T>): T? {
        return instrumentations[type] as? T
    }

    override fun getAll(): Collection<AndroidInstrumentation> {
        return Collections.unmodifiableCollection(instrumentations.values)
    }

    override fun register(instrumentation: AndroidInstrumentation) {
        if (instrumentation::class.java in instrumentations) {
            throw IllegalStateException("Instrumentation with type '${instrumentation::class.java}' already exists.")
        }
        instrumentations[instrumentation.javaClass] = instrumentation
    }
}
