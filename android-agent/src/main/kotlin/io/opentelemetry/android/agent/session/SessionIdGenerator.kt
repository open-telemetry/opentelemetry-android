/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

internal fun interface SessionIdGenerator {
    fun generateSessionId(): String
}
