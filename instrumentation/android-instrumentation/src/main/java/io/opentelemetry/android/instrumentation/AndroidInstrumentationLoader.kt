/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import io.opentelemetry.android.instrumentation.internal.AndroidInstrumentationLoaderImpl

/**
 * Loads and provides [AndroidInstrumentation] instances from the runtime classpath.
 */
interface AndroidInstrumentationLoader {
    /**
     * Provides a single instrumentation if available.
     *
     * @param type The type of the instrumentation to retrieve.
     * @return The instrumentation instance if available, null otherwise.
     */
    fun <T : AndroidInstrumentation> getByType(type: Class<out T>): T?

    /**
     * Provides all registered instrumentations.
     *
     * @return All registered instrumentations.
     */
    fun getAll(): Collection<AndroidInstrumentation>

    companion object {
        private var instance: AndroidInstrumentationLoader? = null

        @JvmStatic
        fun get(): AndroidInstrumentationLoader {
            if (instance == null) {
                instance = AndroidInstrumentationLoaderImpl()
            }
            return instance!!
        }

        /**
         * Convenience method for [AndroidInstrumentationLoader.getByType].
         */
        @JvmStatic
        fun <T : AndroidInstrumentation> getInstrumentation(type: Class<out T>): T = get().getByType(type)!!

        @JvmStatic
        fun resetForTest() {
            instance = null
        }
    }
}
