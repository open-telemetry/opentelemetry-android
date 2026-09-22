/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

/** Records user interaction after checking whether the current session has expired. */
internal fun interface SessionUserInteractionRecorder {
    fun recordUserInteraction()
}
