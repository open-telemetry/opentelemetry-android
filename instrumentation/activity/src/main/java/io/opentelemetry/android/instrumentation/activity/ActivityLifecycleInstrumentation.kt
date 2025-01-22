/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.os.Build
import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer
import io.opentelemetry.android.instrumentation.common.Constants.INSTRUMENTATION_SCOPE
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.internal.services.Services
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

    override fun install(ctx: InstallationContext) {
        startupTimer.start(ctx.openTelemetry.getTracer(INSTRUMENTATION_SCOPE))
        ctx.application.registerActivityLifecycleCallbacks(startupTimer.createLifecycleCallback())
        ctx.application.registerActivityLifecycleCallbacks(buildActivityLifecycleTracer(ctx))
    }

    private fun buildActivityLifecycleTracer(ctx: InstallationContext): DefaultingActivityLifecycleCallbacks {
        val visibleScreenService = Services.get(ctx.application).visibleScreenTracker
        val delegateTracer: Tracer = ctx.openTelemetry.getTracer(INSTRUMENTATION_SCOPE)
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
