/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodicwork

import io.opentelemetry.android.internal.services.Service

interface PeriodicWork : Service {
    fun enqueue(runnable: Runnable)

    companion object {
        /**
         * Default loop interval in milliseconds. This determines how often the periodic work
         * queue is checked for pending tasks. This interval controls the granularity of task
         * scheduling and should be set based on the specific use case and performance requirements.
         */
        const val DEFAULT_LOOP_INTERVAL_MILLIS: Long = 10000L // 10 seconds
    }
}
