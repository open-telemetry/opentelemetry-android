/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Application
import android.content.Context
import android.os.Build
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
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

    override fun install(context: Context, openTelemetryRum: OpenTelemetryRum) {
        startupTimer.start(openTelemetryRum.openTelemetry.getTracer(INSTRUMENTATION_SCOPE), openTelemetryRum.clock)
        (context as? Application)?.let {
            startupLifecycle =
                startupTimer.createLifecycleCallback().apply {
                    it.registerActivityLifecycleCallbacks(this)
                }
            activityLifecycle =
                buildActivityLifecycleTracer(context, openTelemetryRum).apply {
                    it.registerActivityLifecycleCallbacks(this)
                }
        }
    }

    override fun uninstall(context: Context, openTelemetryRum: OpenTelemetryRum) {
        (context as? Application)?.let {
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

    private fun buildActivityLifecycleTracer(context: Context, openTelemetryRum: OpenTelemetryRum): DefaultingActivityLifecycleCallbacks {
        val visibleScreenService = Services.get(context).visibleScreenTracker
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
