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
         * queue is checked for pending tasks. Note that the actual execution of scheduled work
         * may be delayed beyond this interval depending on when tasks are enqueued and their
         * minimum delay requirements.
         */
        const val DEFAULT_LOOP_INTERVAL_MILLIS: Long = 60000L // 1 minute
    }
}
