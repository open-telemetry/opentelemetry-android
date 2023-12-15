/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.tools.time

internal class DefaultSystemTime : SystemTime {
    override fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
