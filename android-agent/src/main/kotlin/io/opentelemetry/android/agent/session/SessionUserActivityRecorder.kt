/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

/** Records user activity after checking whether the current session has expired. */
internal fun interface SessionUserActivityRecorder {
    fun recordUserActivity()
}
