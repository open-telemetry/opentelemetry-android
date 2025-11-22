/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import android.os.Looper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@ExtendWith(MockKExtension::class)
internal class AnrDetectorTest {
    @MockK
    private lateinit var mainLooper: Looper

    @RelaxedMockK
    private lateinit var scheduler: ScheduledExecutorService

    @RelaxedMockK
    private lateinit var appLifecycle: AppLifecycle

    @Test
    fun shouldInstallInstrumentation() {
        every { mainLooper.thread } returns Thread()
        val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder().build()

        val extractor =
            EventAttributesExtractor { _, _: Array<StackTraceElement> ->
                Attributes.of(AttributeKey.stringKey("test.key"), "abc")
            }
        val anrDetector =
            AnrDetector(
                mutableListOf(extractor),
                mainLooper,
                scheduler,
                appLifecycle,
                openTelemetry,
                SessionProvider.getNoop(),
            )
        anrDetector.start()

        // verify that the ANR scheduler was started
        verify(exactly = 1) {
            scheduler.scheduleWithFixedDelay(
                any<AnrWatcher>(),
                1L,
                1L,
                TimeUnit.SECONDS,
            )
        }

        // verify that an application listener was installed
        verify(exactly = 1) {
            appLifecycle.registerListener(any())
        }
    }
}
