/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.powersavemode

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PowerSaveModeInstrumentationTest {
    @get:Rule
    val openTelemetryRule: OpenTelemetryRule = OpenTelemetryRule.create()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val intent = Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)

    // The RUM wrapper is only a vehicle for the OpenTelemetry instance; the assertions below run
    // against the real in-memory telemetry produced by OpenTelemetryRule, not against the mock.
    private val openTelemetryRum = mockk<OpenTelemetryRum>(relaxed = true)
    private val instrumentation = PowerSaveModeInstrumentation()

    @Before
    fun setup() {
        every { openTelemetryRum.openTelemetry } returns openTelemetryRule.openTelemetry
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `emits a power save mode event for broadcasts received after install`() {
        shadowOf(powerManager).setIsPowerSaveMode(true)
        instrumentation.install(context, openTelemetryRum)

        context.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(openTelemetryRule.logRecords).hasSize(1)
        val record = openTelemetryRule.logRecords.single()
        assertThat(record.eventName).isEqualTo(PowerSaveModeDetector.EVENT_NAME)
        assertThat(record.attributes.get(PowerSaveModeDetector.POWER_SAVE_MODE_ENABLED))
            .isEqualTo(true)
    }

    @Test
    fun `stops emitting events after uninstall`() {
        instrumentation.install(context, openTelemetryRum)
        instrumentation.uninstall(context, openTelemetryRum)

        context.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(openTelemetryRule.logRecords).isEmpty()
    }

    @Test
    fun `uninstall before install is a safe no-op`() {
        instrumentation.uninstall(context, openTelemetryRum)

        context.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(openTelemetryRule.logRecords).isEmpty()
    }
}
