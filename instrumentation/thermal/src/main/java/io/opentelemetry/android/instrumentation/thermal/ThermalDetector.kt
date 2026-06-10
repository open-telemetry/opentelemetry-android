/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.thermal

import android.os.PowerManager
import androidx.annotation.RequiresApi
import io.opentelemetry.api.logs.Logger

/**
 * Detects and logs thermal status changes in the Android application.
 *
 * This detector registers as a [PowerManager.OnThermalStatusChangedListener] to listen for
 * changes to the device's thermal throttling status. When a change occurs, it emits a log
 * event via the provided [Logger].
 *
 * The thermal status APIs are only available on API level 29 (Android Q) and higher.
 *
 * @param logger The [Logger] instance used to record thermal status change events.
 */
@RequiresApi(29)
internal class ThermalDetector(
    private val logger: Logger,
) : PowerManager.OnThermalStatusChangedListener {
    internal companion object {
        const val EVENT_NAME = "device.thermal_status.change"
        const val THERMAL_THROTTLING_STATUS = "android.thermal.throttling_status"
    }

    override fun onThermalStatusChanged(status: Int) {
        logger
            .logRecordBuilder()
            .setEventName(EVENT_NAME)
            .setAttribute(THERMAL_THROTTLING_STATUS, status.name)
            .emit()
    }

    private val Int.name: String
        get() {
            return when (this) {
                PowerManager.THERMAL_STATUS_NONE -> "none"
                PowerManager.THERMAL_STATUS_LIGHT -> "light"
                PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
                else -> "unknown"
            }
        }
}
