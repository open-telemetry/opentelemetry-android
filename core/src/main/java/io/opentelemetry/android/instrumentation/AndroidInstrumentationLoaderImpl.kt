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
        ServiceLoader
            .load(AndroidInstrumentation::class.java)
            .associateBy { it.javaClass }
            .toMutableMap()
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
