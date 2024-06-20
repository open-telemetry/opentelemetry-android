/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.OpenTelemetry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class SlowRenderingInstrumentationTest {
    private lateinit var slowRenderingInstrumentation: SlowRenderingInstrumentation
    private lateinit var application: Application
    private lateinit var openTelemetryRum: OpenTelemetryRum
    private lateinit var openTelemetry: OpenTelemetry

    @Before
    fun setUp() {
        application = mockk()
        openTelemetry = mockk()
        openTelemetryRum = mockk()
        every { openTelemetryRum.openTelemetry }.returns(openTelemetry)
        slowRenderingInstrumentation = SlowRenderingInstrumentation()
    }

    @Test
    fun `Verify default poll interval value`() {
        assertThat(slowRenderingInstrumentation.slowRenderingDetectionPollInterval).isEqualTo(
            Duration.ofSeconds(1),
        )
    }

    @Test
    fun `Changing poll interval value`() {
        slowRenderingInstrumentation.setSlowRenderingDetectionPollInterval(Duration.ofSeconds(2))

        assertThat(slowRenderingInstrumentation.slowRenderingDetectionPollInterval).isEqualTo(
            Duration.ofSeconds(2),
        )
    }

    @Test
    fun `Not changing poll interval value when provided value is negative`() {
        slowRenderingInstrumentation.setSlowRenderingDetectionPollInterval(Duration.ofSeconds(-2))

        assertThat(slowRenderingInstrumentation.slowRenderingDetectionPollInterval).isEqualTo(
            Duration.ofSeconds(1),
        )
    }

    @Config(sdk = [23])
    @Test
    fun `Not installing instrumentation on devices with API level lower than 24`() {
        slowRenderingInstrumentation.install(application, openTelemetryRum)

        verify {
            application wasNot Called
        }
        verify {
            openTelemetryRum wasNot Called
        }
    }

    @Config(sdk = [24, 25])
    @Test
    fun `Installing instrumentation on devices with API level equal or higher than 24`() {
        val capturedListener = slot<SlowRenderListener>()
        every { openTelemetry.getTracer(any()) }.returns(mockk())
        every { application.registerActivityLifecycleCallbacks(any()) } just Runs

        slowRenderingInstrumentation.install(application, openTelemetryRum)

        verify { openTelemetry.getTracer("io.opentelemetry.slow-rendering") }
        verify { application.registerActivityLifecycleCallbacks(capture(capturedListener)) }
    }
}
