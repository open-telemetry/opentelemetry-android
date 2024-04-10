/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum

interface AndroidInstrumentation {
    companion object {
        private val registry by lazy { AndroidInstrumentationRegistry.create() }

        @JvmStatic
        fun <T : AndroidInstrumentation> get(type: Class<out T>): T {
            return registry.getByType(type)
        }

        @JvmStatic
        fun getAll(): Collection<AndroidInstrumentation> {
            return registry.getAll()
        }
    }

    fun apply(
        application: Application,
        openTelemetryRum: OpenTelemetryRum,
    )
}
