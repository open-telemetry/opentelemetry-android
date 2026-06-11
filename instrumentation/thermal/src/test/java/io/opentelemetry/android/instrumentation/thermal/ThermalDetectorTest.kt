/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.thermal

import android.os.PowerManager
import io.opentelemetry.android.instrumentation.thermal.ThermalDetector.Companion.EVENT_NAME
import io.opentelemetry.android.instrumentation.thermal.ThermalDetector.Companion.THERMAL_THROTTLING_STATUS
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ThermalDetectorTest {
    private lateinit var detector: ThermalDetector

    @get:Rule
    val openTelemetryRule: OpenTelemetryRule = OpenTelemetryRule.create()

    private val logger =
        openTelemetryRule.openTelemetry
            .logsBridge
            .loggerBuilder("io.opentelemetry.test")
            .build()

    @Before
    fun setup() {
        detector = ThermalDetector(logger)
    }

    @Test
    fun `should emit thermal state change event on status change`() {
        // when
        detector.onThermalStatusChanged(PowerManager.THERMAL_STATUS_SEVERE)

        // then
        assertThat(openTelemetryRule.logRecords).hasSize(1)
        val record = openTelemetryRule.logRecords.single()
        assertThat(record.eventName).isEqualTo(EVENT_NAME)
        assertThat(record.attributes.get(AttributeKey.stringKey(THERMAL_THROTTLING_STATUS)))
            .isEqualTo("severe")
    }

    @Test
    fun `should map every known thermal status code to its readable name`() {
        // given
        val expectedNames =
            mapOf(
                PowerManager.THERMAL_STATUS_NONE to "none",
                PowerManager.THERMAL_STATUS_LIGHT to "light",
                PowerManager.THERMAL_STATUS_MODERATE to "moderate",
                PowerManager.THERMAL_STATUS_SEVERE to "severe",
                PowerManager.THERMAL_STATUS_CRITICAL to "critical",
                PowerManager.THERMAL_STATUS_EMERGENCY to "emergency",
                PowerManager.THERMAL_STATUS_SHUTDOWN to "shutdown",
            )

        // when
        expectedNames.keys.forEach { detector.onThermalStatusChanged(it) }

        // then
        val emitted =
            openTelemetryRule.logRecords.map {
                it.attributes.get(AttributeKey.stringKey(THERMAL_THROTTLING_STATUS))
            }
        assertThat(emitted).containsExactlyElementsOf(expectedNames.values)
    }

    @Test
    fun `should fall back to unknown for an unrecognized status code`() {
        // when
        detector.onThermalStatusChanged(Int.MAX_VALUE)

        // then
        assertThat(openTelemetryRule.logRecords).hasSize(1)
        assertThat(
            openTelemetryRule.logRecords
                .single()
                .attributes
                .get(AttributeKey.stringKey(THERMAL_THROTTLING_STATUS)),
        ).isEqualTo("unknown")
    }
}
