/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

interface AndroidInstrumentationRegistry {
    fun <T : AndroidInstrumentation> get(type: Class<out T>): T

    fun getAll(): Collection<AndroidInstrumentation>

    fun register(instrumentation: AndroidInstrumentation)
}
