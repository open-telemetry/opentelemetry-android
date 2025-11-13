/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Application
import android.os.Build
import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer
import io.opentelemetry.android.instrumentation.common.Constants.INSTRUMENTATION_SCOPE
import io.opentelemetry.android.instrumentation.common.DefaultScreenNameExtractor
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks
import io.opentelemetry.api.trace.Tracer

@AutoService(AndroidInstrumentation::class)
class ActivityLifecycleInstrumentation : AndroidInstrumentation {
    private val startupTimer: AppStartupTimer by lazy { AppStartupTimer() }
    private var screenNameExtractor: ScreenNameExtractor = DefaultScreenNameExtractor
    private var tracerCustomizer: (Tracer) -> Tracer = { it }
    private var startupLifecycle: Application.ActivityLifecycleCallbacks? = null
    private var activityLifecycle: Application.ActivityLifecycleCallbacks? = null

    override val name: String = "activity"

    fun setTracerCustomizer(customizer: (Tracer) -> Tracer) {
        tracerCustomizer = customizer
    }

    fun setScreenNameExtractor(screenNameExtractor: ScreenNameExtractor) {
        this.screenNameExtractor = screenNameExtractor
    }

    override fun install(ctx: InstallationContext) {
        startupTimer.start(ctx.openTelemetry.getTracer(INSTRUMENTATION_SCOPE))
        ctx.application?.let {
            startupLifecycle =
                startupTimer.createLifecycleCallback().apply {
                    it.registerActivityLifecycleCallbacks(this)
                }
            activityLifecycle =
                buildActivityLifecycleTracer(ctx).apply {
                    it.registerActivityLifecycleCallbacks(this)
                }
        }
    }

    override fun uninstall(ctx: InstallationContext) {
        ctx.application?.let {
            if (startupLifecycle != null) {
                it.unregisterActivityLifecycleCallbacks(startupLifecycle)
                startupLifecycle = null
            }
            if (activityLifecycle != null) {
                it.unregisterActivityLifecycleCallbacks(activityLifecycle)
                activityLifecycle = null
            }
        }
    }

    private fun buildActivityLifecycleTracer(ctx: InstallationContext): DefaultingActivityLifecycleCallbacks {
        val visibleScreenService = Services.get(ctx.context).visibleScreenTracker
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
