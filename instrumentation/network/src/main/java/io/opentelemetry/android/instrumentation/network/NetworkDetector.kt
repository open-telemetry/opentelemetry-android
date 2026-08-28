/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network

import android.content.Context

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
internal interface NetworkDetector {
    fun detectCurrentNetwork(): CurrentNetwork

    companion object {
        @JvmStatic
        fun create(context: Context): NetworkDetector = NetworkDetectorImpl(context)
    }
}
