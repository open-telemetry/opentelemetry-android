/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import io.opentelemetry.android.internal.instrumentation.AndroidInstrumentationServicesImpl

/**
 * Loads and provides Java SPI services of type [AndroidInstrumentation] from the runtime classpath.
 */
interface AndroidInstrumentationServices {
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
        private var instance: AndroidInstrumentationServices? = null

        @JvmStatic
        fun get(): AndroidInstrumentationServices {
            if (instance == null) {
                instance = AndroidInstrumentationServicesImpl()
            }
            return instance!!
        }

        /**
         * Convenience method for [AndroidInstrumentationServices.getByType].
         */
        @JvmStatic
        fun <T : AndroidInstrumentation> getService(type: Class<out T>): T? {
            return get().getByType(type)
        }

        @JvmStatic
        fun resetForTest() {
            instance = null
        }
    }
}
