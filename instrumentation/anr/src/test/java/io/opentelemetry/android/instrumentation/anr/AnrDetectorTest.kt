/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import android.os.Looper
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
internal class AnrDetectorTest {
    @Mock
    private lateinit var mainLooper: Looper

    @Mock
    private lateinit var scheduler: ScheduledExecutorService

    @Mock
    private lateinit var appLifecycle: AppLifecycle

    @Test
    fun shouldInstallInstrumentation() {
        Mockito.`when`(mainLooper.thread).thenReturn(Thread())
        val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder().build()

        val extractor =
            EventAttributesExtractor { parentContext: Context, o: Array<StackTraceElement> ->
                Attributes.of(AttributeKey.stringKey("test.key"), "abc")
            }
        val anrDetector =
            AnrDetector(
                mutableListOf(extractor),
                mainLooper,
                scheduler,
                appLifecycle,
                openTelemetry,
            )
        anrDetector.start()

        // verify that the ANR scheduler was started
        Mockito
            .verify(scheduler)
            .scheduleWithFixedDelay(
                ArgumentMatchers.isA(AnrWatcher::class.java),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(
                    TimeUnit.SECONDS,
                ),
            )
    }
}
