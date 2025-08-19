/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.api.common.Attributes
import java.io.File

/** Represents details about the runtime environment at a time  */
internal class RuntimeDetailsExtractor(
    private val filesDir: File,
) : BroadcastReceiver(),
    EventAttributesExtractor<CrashDetails> {
    @Volatile
    private var currentBatteryPercent: Double? = null

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        this.currentBatteryPercent = level * 100.0 / scale.toFloat()
    }

    override fun extract(
        parentContext: io.opentelemetry.context.Context,
        subject: CrashDetails,
    ): Attributes {
        val attributes = Attributes.builder()
        attributes.put(
            RumConstants.STORAGE_SPACE_FREE_KEY,
            this.getCurrentStorageFreeSpaceInBytes(),
        )
        attributes.put(RumConstants.HEAP_FREE_KEY, this.getCurrentFreeHeapInBytes())

        this.currentBatteryPercent?.let {
            attributes.put(RumConstants.BATTERY_PERCENT_KEY, it)
        }
        return attributes.build()
    }

    private fun getCurrentStorageFreeSpaceInBytes(): Long = filesDir.freeSpace

    private fun getCurrentFreeHeapInBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.freeMemory()
    }

    companion object {
        fun create(context: Context): RuntimeDetailsExtractor {
            val batteryChangedFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val filesDir = context.filesDir
            val runtimeDetails = RuntimeDetailsExtractor(filesDir)
            context.registerReceiver(runtimeDetails, batteryChangedFilter)
            return runtimeDetails
        }
    }
}
