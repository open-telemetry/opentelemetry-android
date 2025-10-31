/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.screenorientation

import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.instrumentation.screenorientation.ScreenOrientationDetector.Companion.EVENT_NAME
import io.opentelemetry.android.instrumentation.screenorientation.ScreenOrientationDetector.Companion.SCREEN_ORIENTATION
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.assertNotNull

class ScreenOrientationDetectorTest {
    private lateinit var sut: ScreenOrientationDetector

    @get:Rule
    val openTelemetryRule: OpenTelemetryRule = OpenTelemetryRule.create()

    private val logger =
        openTelemetryRule.openTelemetry
            .logsBridge
            .loggerBuilder("io.opentelemetry.test")
            .build()
    private val applicationContext = mockk<Context>()

    @Before
    fun setup() {
        every { applicationContext.resources } returns
            mockk(relaxed = true) {
                every { configuration } returns
                    Configuration().apply {
                        orientation = ORIENTATION_PORTRAIT
                    }
            }

        sut =
            ScreenOrientationDetector(
                applicationContext,
                logger,
            )
    }

    @Test
    fun `should emit orientation change event on configuration change`() {
        // given
        sut.onConfigurationChanged(
            Configuration().apply {
                orientation = Configuration.ORIENTATION_LANDSCAPE
            },
        )

        // then
        val record = openTelemetryRule.logRecords.find { it.eventName === EVENT_NAME }
        assertEquals(1, openTelemetryRule.logRecords.size)
        assertNotNull(record)
        assertEquals("landscape", record.attributes.get(AttributeKey.stringKey(SCREEN_ORIENTATION)))
    }

    @Test
    fun `should not emit orientation change event if orientation is the same`() {
        // given
        sut.onConfigurationChanged(
            Configuration().apply {
                orientation = ORIENTATION_PORTRAIT
            },
        )

        // then
        assertEquals(0, openTelemetryRule.logRecords.size)
        assertNull(openTelemetryRule.logRecords.find { it.eventName === EVENT_NAME })
    }
}
