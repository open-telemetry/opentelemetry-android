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
     * @return The instrumentation instance if available, NULL otherwise.
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
        private val instance: AndroidInstrumentationRegistry by lazy {
            AndroidInstrumentationRegistryImpl()
        }

        @JvmStatic
        fun get(): AndroidInstrumentationRegistry {
            return instance
        }
    }
}
