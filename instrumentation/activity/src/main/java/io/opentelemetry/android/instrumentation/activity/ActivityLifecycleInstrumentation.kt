/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Application
import android.os.Build
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer
import io.opentelemetry.android.instrumentation.common.Constants.INSTRUMENTATION_SCOPE
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks
import io.opentelemetry.api.trace.Tracer

@AutoService(AndroidInstrumentation::class)
class ActivityLifecycleInstrumentation : AndroidInstrumentation {
    private val startupTimer: AppStartupTimer by lazy { AppStartupTimer() }
    private var screenNameExtractor: ScreenNameExtractor = ScreenNameExtractor.DEFAULT
    private var tracerCustomizer: (Tracer) -> Tracer = { it }

    fun setTracerCustomizer(customizer: (Tracer) -> Tracer) {
        tracerCustomizer = customizer
    }

    fun setScreenNameExtractor(screenNameExtractor: ScreenNameExtractor) {
        this.screenNameExtractor = screenNameExtractor
    }

    override fun install(
        application: Application,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        application.registerActivityLifecycleCallbacks(startupTimer.createLifecycleCallback())
        application.registerActivityLifecycleCallbacks(buildActivityLifecycleTracer(openTelemetryRum))
    }

    private fun buildActivityLifecycleTracer(openTelemetryRum: OpenTelemetryRum): DefaultingActivityLifecycleCallbacks {
        val visibleScreenService = ServiceManager.get().getVisibleScreenService()
        val delegateTracer: Tracer = openTelemetryRum.openTelemetry.getTracer(INSTRUMENTATION_SCOPE)
        val tracers =
            ActivityTracerCache(
                tracerCustomizer.invoke(delegateTracer),
                visibleScreenService,
                startupTimer,
                screenNameExtractor,
            )
        return if (Build.VERSION.SDK_INT < 29) {
            Pre29ActivityCallbacks(tracers)
        } else {
            ActivityCallbacks(tracers)
        }
    }
}
