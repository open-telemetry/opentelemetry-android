/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

/**
 * Configures a discovered [AndroidInstrumentation] before it is installed.
 *
 * @param T The instrumentation type this configurator can configure.
 */
interface InstrumentationConfigurator<T : AndroidInstrumentation> {
    /**
     * The Class of the AndroidInstrumentation that this class can configure.
     */
    val instrumentationType: Class<T>

    /**
     * Called to configure an [instrumentation] before [AndroidInstrumentation.install] is called.
     */
    fun configure(instrumentation: T)
}
