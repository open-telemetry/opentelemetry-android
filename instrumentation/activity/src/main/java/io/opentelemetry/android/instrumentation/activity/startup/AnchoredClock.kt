/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.startup

import io.opentelemetry.sdk.common.Clock

internal class AnchoredClock(
    private val clock: Clock,
    private val epochNanos: Long = clock.now(),
    private val nanoTime: Long = clock.nanoTime(),
) {
    fun now(): Long {
        val deltaNanos = clock.nanoTime() - nanoTime
        return epochNanos + deltaNanos
    }
}
