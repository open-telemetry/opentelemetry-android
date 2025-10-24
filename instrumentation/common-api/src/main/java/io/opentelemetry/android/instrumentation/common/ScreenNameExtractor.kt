/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common

fun interface ScreenNameExtractor {
    /**
     * Obtains a screen name from the supplied instance.
     */
    fun extract(instance: Any): String
}
