/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

/**
 * Opt-in interface for [AndroidInstrumentation] implementations that require specific relative
 * installation ordering.
 *
 * Instrumentations that do not implement this interface are installed in their default discovery order.
 */
interface OrderedAndroidInstrumentation : AndroidInstrumentation {
    /**
     * If true, this instrumentation should be installed before other instrumentations that do not declare
     * [installFirst], unless explicit ordering constraints indicate otherwise.
     */
    val installFirst: Boolean get() = false

    /**
     * Set of instrumentation names (from [AndroidInstrumentation.name]) that must be installed
     * BEFORE this instrumentation is installed.
     *
     * In other words, this instrumentation will be installed AFTER the instrumentations listed here.
     */
    val installAfter: Set<String> get() = emptySet()

    /**
     * Set of instrumentation names (from [AndroidInstrumentation.name]) that must be installed
     * AFTER this instrumentation is installed.
     *
     * In other words, this instrumentation will be installed BEFORE the instrumentations listed here.
     */
    val installBefore: Set<String> get() = emptySet()
}
