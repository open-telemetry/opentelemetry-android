/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.powersavemode

import android.content.Context
import android.content.IntentFilter
import android.os.PowerManager
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation

/**
 * An Android instrumentation module that installs and manages [PowerSaveModeDetector].
 *
 * The power save mode APIs ([PowerManager.isPowerSaveMode] and
 * [PowerManager.ACTION_POWER_SAVE_MODE_CHANGED]) are available since API level 21, which is below
 * this library's minSdk, so no version gating is required.
 */
@AutoService(AndroidInstrumentation::class)
class PowerSaveModeInstrumentation : AndroidInstrumentation {
    private var detector: PowerSaveModeDetector? = null

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val applicationContext = context.applicationContext
        val powerManager =
            applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: return

        val logger =
            openTelemetryRum.openTelemetry
                .logsBridge
                .loggerBuilder("io.opentelemetry.$name")
                .build()

        val detector = PowerSaveModeDetector(powerManager, logger)
        this.detector = detector
        applicationContext.registerReceiver(
            detector,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
        )
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val detector = this.detector ?: return
        context.applicationContext.unregisterReceiver(detector)
        this.detector = null
    }

    override val name: String = "power_save_mode"
}
