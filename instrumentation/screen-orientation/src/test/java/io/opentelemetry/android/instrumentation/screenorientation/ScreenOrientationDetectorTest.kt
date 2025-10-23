package io.opentelemetry.android.instrumentation.screenorientation

import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.instrumentation.screenorientation.ScreenOrientationDetector.Companion.EVENT_NAME
import io.opentelemetry.android.instrumentation.screenorientation.model.Orientation
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertEquals
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.assertNotNull

class ScreenOrientationDetectorTest {
    private lateinit var sut: ScreenOrientationDetector

    @get:Rule
    val openTelemetryRule: OpenTelemetryRule = OpenTelemetryRule.create();

    private val logger = openTelemetryRule.openTelemetry
        .logsBridge
        .loggerBuilder("io.opentelemetry.test")
        .build()
    private val applicationContext = mockk<Context>()
    private val testAttributeKey = AttributeKey.stringKey("test-key")
    private val testValue = "test-value"
    private val additionalExtractors = listOf(TestExtractor())


    @Before
    fun setup() {
        every { applicationContext.resources } returns mockk(relaxed = true) {
            every { configuration } returns Configuration().apply {
                orientation = ORIENTATION_PORTRAIT
            }
        }

        sut = ScreenOrientationDetector(
            applicationContext,
            logger,
            additionalExtractors
        )
    }

    @Test
    fun `should emit orientation change event on configuration change`() {
        // given
        sut.onConfigurationChanged(Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        })

        // then
        assertEquals(1, openTelemetryRule.logRecords.size)
        assertNotNull(openTelemetryRule.logRecords.find { it.eventName === EVENT_NAME })
    }

    @Test
    fun `should not emit orientation change event if orientation is the same`() {
        // given
        sut.onConfigurationChanged(Configuration().apply {
            orientation = ORIENTATION_PORTRAIT
        })

        // then
        assertEquals(0, openTelemetryRule.logRecords.size)
        assertNull(openTelemetryRule.logRecords.find { it.eventName === EVENT_NAME })
    }

    @Test
    fun `should add attributes from additional extractors`() {
        // given
        sut.onConfigurationChanged(Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        })

        // then
        val logRecord = openTelemetryRule.logRecords.find { it.eventName === EVENT_NAME }
        assertNotNull(logRecord)
        assertEquals(testValue, logRecord.attributes.get(testAttributeKey))
    }

    private inner class TestExtractor : EventAttributesExtractor<Orientation> {
        override fun extract(
            parentContext: io.opentelemetry.context.Context,
            subject: Orientation
        ): Attributes {
            return Attributes.builder().put(testAttributeKey, testValue).build()
        }
    }
}
