/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.api.logs.LoggerProvider
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class SlowRenderingInstrumentationTest {
    private lateinit var slowRenderingInstrumentation: SlowRenderingInstrumentation

    @MockK
    private lateinit var application: Application

    @MockK
    private lateinit var openTelemetry: OpenTelemetrySdk

    @MockK
    private lateinit var logger: Logger

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val logsBridge: LoggerProvider = mockk()
        slowRenderingInstrumentation = SlowRenderingInstrumentation()
        every { openTelemetry.logsBridge } returns logsBridge
        every { logsBridge.get("app.jank") } returns logger
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
        val ctx = InstallationContext(application, openTelemetry, mockk())
        slowRenderingInstrumentation.install(ctx)

        verify {
            application wasNot Called
        }
        verify {
            openTelemetry wasNot Called
        }
    }

    @Config(sdk = [24, 25])
    @Test
    fun `Installing instrumentation on devices with API level equal or higher than 24`() {
        val capturedListener = slot<SlowRenderListener>()
        every { openTelemetry.getTracer(any()) }.returns(mockk())
        every { application.registerActivityLifecycleCallbacks(any()) } just Runs
        val ctx = InstallationContext(application, openTelemetry, mockk())
        slowRenderingInstrumentation.install(ctx)

        verify { application.registerActivityLifecycleCallbacks(capture(capturedListener)) }
    }

    @Config(sdk = [24, 25])
    @Test
    fun `can use legacy span`() {
        val capturedListener = slot<SlowRenderListener>()
        every { openTelemetry.getTracer(any()) }.returns(mockk())
        every { application.registerActivityLifecycleCallbacks(any()) } just Runs
        val ctx = InstallationContext(application, openTelemetry, mockk())
        slowRenderingInstrumentation.enableDeprecatedZeroDurationSpan().install(ctx)

        verify { openTelemetry.getTracer("io.opentelemetry.slow-rendering") }
        verify { application.registerActivityLifecycleCallbacks(capture(capturedListener)) }
    }
}
