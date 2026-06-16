/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.powersavemode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.logs.Logger

/**
 * Detects and logs changes to the device's power save (battery saver) mode.
 *
 * This detector is registered for [PowerManager.ACTION_POWER_SAVE_MODE_CHANGED]; on each broadcast
 * it reads [PowerManager.isPowerSaveMode] and emits one event via the provided [Logger].
 *
 * @param powerManager The [PowerManager] used to read the current power save mode state.
 * @param logger The [Logger] instance used to record power save mode change events.
 */
internal class PowerSaveModeDetector(
    private val powerManager: PowerManager,
    private val logger: Logger,
) : BroadcastReceiver() {
    internal companion object {
        const val EVENT_NAME = "device.power_save_mode.change"
        val POWER_SAVE_MODE_ENABLED: AttributeKey<Boolean> =
            AttributeKey.booleanKey("android.power_save_mode.enabled")
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        logger
            .logRecordBuilder()
            .setEventName(EVENT_NAME)
            .setAttribute(POWER_SAVE_MODE_ENABLED, powerManager.isPowerSaveMode)
            .emit()
    }
}
