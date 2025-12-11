/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.util.Log
import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.internal.services.Services

/**
 * OpenTelemetry instrumentation for Android PANS (Per-Application Network Selection) metrics.
 *
 * This instrumentation automatically collects and exposes metrics related to per-app network usage,
 * network types (OEM_PAID, OEM_PRIVATE), and network preference changes as OpenTelemetry metrics.
 */
@AutoService(AndroidInstrumentation::class)
class PansInstrumentation : AndroidInstrumentation {
    private var metricsCollector: PansMetricsCollector? = null

    override val name: String = "pans"

    override fun install(ctx: InstallationContext) {
        try {
            // Verify that Services are available
            Services.get(ctx.context)

            // Create and start the metrics collector
            metricsCollector =
                PansMetricsCollector(
                    context = ctx.context,
                    sdk = ctx.openTelemetry as io.opentelemetry.sdk.OpenTelemetrySdk,
                    collectionIntervalMinutes = DEFAULT_COLLECTION_INTERVAL_MINUTES,
                )

            metricsCollector?.start()

            Log.i(TAG, "PANS instrumentation installed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install PANS instrumentation", e)
            // Don't rethrow - allow other instrumentations to continue
        }
    }

    companion object {
        private const val TAG = "PansInstrumentation"
        private const val DEFAULT_COLLECTION_INTERVAL_MINUTES = 15L
    }
}
