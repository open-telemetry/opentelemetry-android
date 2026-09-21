/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

/** Records activity after checking whether the current session has expired. */
internal fun interface SessionActivityRecorder {
    fun recordActivity()
}
