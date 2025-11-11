/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.tools.time

import android.os.SystemClock
import io.opentelemetry.sdk.common.Clock
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class AndroidClock : Clock by Clock.getDefault() {
    override fun nanoTime(): Long = SystemClock.elapsedRealtimeNanos()

    companion object {
        @JvmStatic
        val INSTANCE = AndroidClock()
    }
}
