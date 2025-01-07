/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.tools.time

/**
 * Utility to be able to mock the current system time for testing purposes.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
interface SystemTime {
    companion object {
        private var instance: SystemTime = DefaultSystemTime()

        fun get(): SystemTime = instance

        fun setForTest(instance: SystemTime) {
            this.instance = instance
        }
    }

    fun getCurrentTimeMillis(): Long

    class DefaultSystemTime : SystemTime {
        override fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
    }
}
