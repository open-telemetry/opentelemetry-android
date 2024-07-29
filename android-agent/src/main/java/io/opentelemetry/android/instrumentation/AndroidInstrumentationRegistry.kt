/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

/**
 * Stores and provides all the available instrumentations.
 */
interface AndroidInstrumentationRegistry {
    /**
     * Provides a single instrumentation if available.
     *
     * @param type The type of the instrumentation to retrieve.
     * @return The instrumentation instance if available, null otherwise.
     */
    fun <T : AndroidInstrumentation> get(type: Class<out T>): T?

    /**
     * Provides all registered instrumentations.
     *
     * @return All registered instrumentations.
     */
    fun getAll(): Collection<AndroidInstrumentation>

    /**
     * Stores an instrumentation as long as there is not other instrumentation already registered with the same
     * type.
     *
     * @param instrumentation The instrumentation to register.
     * @throws IllegalStateException If the instrumentation couldn't be registered.
     */
    fun register(instrumentation: AndroidInstrumentation)

    companion object {
        private var instance: AndroidInstrumentationRegistry? = null

        @JvmStatic
        fun get(): AndroidInstrumentationRegistry {
            if (instance == null) {
                instance = AndroidInstrumentationRegistryImpl()
            }
            return instance!!
        }

        @JvmStatic
        fun resetForTest() {
            instance = null
        }
    }
}
