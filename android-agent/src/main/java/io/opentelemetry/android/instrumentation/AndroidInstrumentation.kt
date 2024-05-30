/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum
import java.util.Collections
import java.util.ServiceLoader

/**
 * This interface defines a tool that automatically generates telemetry, that is, creates spans, metrics or logs for a specific use-case
 * without the need for end users to directly interact with the OpenTelemetry SDK to create those signals manually.
 *
 * Implementations of this interface should be focused on a single use-case and should attach themselves automatically
 * to the tool that they are supposed to generate telemetry for. For example, an implementation that tracks
 * Fragment lifecycle methods by generating OTel events in key places of a Fragment's lifecycle, should
 * come with its own "FragmentLifecycleCallbacks" implementation (or similar callback mechanism that notifies when a fragment lifecycle state has changed)
 * and should find a way to register its callback into all of the Fragments of the host app to automatically
 * track their lifecycle without end users having to modify their project's code to make it work.
 *
 * Even though users shouldn't have to write code to make an AndroidInstrumentation implementation work,
 * implementations should expose configurable options whenever possible to allow users to customize relevant
 * options depending on the use-case.
 *
 * Access to an implementation, either to configure it or to install it, must be made through
 * [AndroidInstrumentation.get] or [AndroidInstrumentation.getAll].
 */
interface AndroidInstrumentation {
    /**
     * This is the entry point of the instrumentation, it must be called once per implementation and it should
     * only be called from [OpenTelemetryRum]'s builder once the [OpenTelemetryRum] instance is initialized and ready
     * to use for generating telemetry.
     */
    fun install(
        application: Application,
        openTelemetryRum: OpenTelemetryRum,
    )

    companion object Registry {
        private val instrumentations: MutableMap<Class<out AndroidInstrumentation>, AndroidInstrumentation> by lazy {
            ServiceLoader.load(AndroidInstrumentation::class.java).associateBy { it.javaClass }
                .toMutableMap()
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T : AndroidInstrumentation> get(type: Class<out T>): T {
            return instrumentations.getValue(type) as T
        }

        @JvmStatic
        fun getAll(): Collection<AndroidInstrumentation> {
            return Collections.unmodifiableCollection(instrumentations.values)
        }

        fun register(instrumentation: AndroidInstrumentation) {
            if (instrumentation::class.java in instrumentations) {
                throw IllegalStateException("Instrumentation with type '${instrumentation::class.java}' already exists.")
            }
            instrumentations[instrumentation.javaClass] = instrumentation
        }
    }
}
