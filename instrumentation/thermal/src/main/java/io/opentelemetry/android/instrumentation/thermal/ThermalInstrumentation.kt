/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.thermal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * An Android instrumentation module that installs and manages [ThermalDetector].
 *
 * The underlying thermal status APIs ([PowerManager.addThermalStatusListener]) are only
 * available on API level 29 (Android Q) and higher, so this instrumentation no-ops on older
 * devices even though the library supports a lower minSdk.
 */
@AutoService(AndroidInstrumentation::class)
class ThermalInstrumentation : AndroidInstrumentation {
    private var detector: ThermalDetector? = null
    private var executor: ExecutorService? = null
    private var powerManager: PowerManager? = null

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }

        val applicationContext = context.applicationContext
        val powerManager =
            applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: return

        val logger =
            openTelemetryRum.openTelemetry
                .logsBridge
                .loggerBuilder("io.opentelemetry.$name")
                .build()

        val executor = Executors.newSingleThreadExecutor()
        val detector = ThermalDetector(logger)

        this.powerManager = powerManager
        this.executor = executor
        this.detector = detector

        registerListener(powerManager, executor, detector)
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val powerManager = this.powerManager
        val detector = this.detector
        if (powerManager != null && detector != null) {
            unregisterListener(powerManager, detector)
        }
        executor?.shutdown()
        this.detector = null
        this.executor = null
        this.powerManager = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun registerListener(
        powerManager: PowerManager,
        executor: ExecutorService,
        detector: ThermalDetector,
    ) {
        powerManager.addThermalStatusListener(executor, detector)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun unregisterListener(
        powerManager: PowerManager,
        detector: ThermalDetector,
    ) {
        powerManager.removeThermalStatusListener(detector)
    }

    override val name: String = "thermal"
}
