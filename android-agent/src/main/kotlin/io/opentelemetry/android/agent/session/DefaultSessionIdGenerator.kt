/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.api.trace.TraceId
import kotlin.random.Random

internal class DefaultSessionIdGenerator(
    private val random: Random,
) : SessionIdGenerator {
    override fun generateSessionId(): String {
        // The OTel TraceId has exactly the same format as a RUM SessionId, so let's re-use it here,
        // rather than re-inventing the wheel.
        return TraceId.fromLongs(random.nextLong(), random.nextLong())
    }
}
