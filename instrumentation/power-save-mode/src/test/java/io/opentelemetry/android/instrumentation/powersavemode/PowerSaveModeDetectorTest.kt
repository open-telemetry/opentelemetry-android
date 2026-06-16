/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.powersavemode

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PowerSaveModeDetectorTest {
    @get:Rule
    val openTelemetryRule: OpenTelemetryRule = OpenTelemetryRule.create()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val intent = Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)

    private val logger =
        openTelemetryRule.openTelemetry
            .logsBridge
            .loggerBuilder("io.opentelemetry.test")
            .build()

    private lateinit var detector: PowerSaveModeDetector

    @Before
    fun setup() {
        detector = PowerSaveModeDetector(powerManager, logger)
    }

    @Test
    fun `should emit an event with enabled true when power save mode is on`() {
        // given
        shadowOf(powerManager).setIsPowerSaveMode(true)

        // when
        detector.onReceive(context, intent)

        // then
        assertThat(openTelemetryRule.logRecords).hasSize(1)
        val record = openTelemetryRule.logRecords.single()
        assertThat(record.eventName).isEqualTo(PowerSaveModeDetector.EVENT_NAME)
        assertThat(record.attributes.get(PowerSaveModeDetector.POWER_SAVE_MODE_ENABLED))
            .isEqualTo(true)
    }

    @Test
    fun `should emit an event with enabled false when power save mode is off`() {
        // given
        shadowOf(powerManager).setIsPowerSaveMode(false)

        // when
        detector.onReceive(context, intent)

        // then
        assertThat(openTelemetryRule.logRecords).hasSize(1)
        val record = openTelemetryRule.logRecords.single()
        assertThat(record.eventName).isEqualTo(PowerSaveModeDetector.EVENT_NAME)
        assertThat(record.attributes.get(PowerSaveModeDetector.POWER_SAVE_MODE_ENABLED))
            .isEqualTo(false)
    }
}
