/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.tools.time

import android.os.SystemClock
import io.opentelemetry.sdk.common.Clock
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class AndroidClock internal constructor(
    timeSinceEpochMillisProvider: () -> Long = System::currentTimeMillis,
    private val timeHighPrecisionMillisProvider: () -> Long = SystemClock::elapsedRealtime,
) : Clock by Clock.getDefault() {
    private val baseline = timeSinceEpochMillisProvider() - timeHighPrecisionMillisProvider()

    override fun nanoTime(): Long = baseline + timeHighPrecisionMillisProvider()

    companion object {
        @JvmStatic
        val INSTANCE = AndroidClock()
    }
}
