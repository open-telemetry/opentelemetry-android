/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import java.util.ServiceLoader

internal class AndroidInstrumentationRegistry(
    private val instrumentations: Map<Class<out AndroidInstrumentation>, AndroidInstrumentation>,
) {
    companion object {
        fun create(): AndroidInstrumentationRegistry {
            val instrumentationMap =
                mutableMapOf<Class<out AndroidInstrumentation>, AndroidInstrumentation>()

            val instrumentations = ServiceLoader.load(AndroidInstrumentation::class.java)
            instrumentations.forEach {
                instrumentationMap[it.javaClass] = it
            }

            return AndroidInstrumentationRegistry(instrumentationMap)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : AndroidInstrumentation> getByType(type: Class<out T>): T {
        return instrumentations.getValue(type) as T
    }

    fun getAll(): Collection<AndroidInstrumentation> {
        return instrumentations.values
    }
}
