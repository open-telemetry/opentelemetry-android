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
import io.opentelemetry.android.internal.services.ServicesAwareInstrumentation
import io.opentelemetry.android.internal.services.ServicesFactory
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer

@AutoService(AndroidInstrumentation::class)
class ActivityLifecycleInstrumentation :
    AndroidInstrumentation,
    ServicesAwareInstrumentation {
    private lateinit var visibleScreenTracker: VisibleScreenTracker
    private val startupTimer: AppStartupTimer by lazy { AppStartupTimer() }
    private var screenNameExtractor: ScreenNameExtractor = DefaultScreenNameExtractor
    private var tracerCustomizer: (Tracer) -> Tracer = { it }
    private var startupLifecycle: Application.ActivityLifecycleCallbacks? = null
    private var activityLifecycle: Application.ActivityLifecycleCallbacks? = null

    override val name: String = "activity"

    override fun configureServices(services: ServicesFactory) {
        visibleScreenTracker = services.visibleScreenTracker
    }

    fun setTracerCustomizer(customizer: (Tracer) -> Tracer) {
        tracerCustomizer = customizer
    }

    fun setScreenNameExtractor(screenNameExtractor: ScreenNameExtractor) {
        this.screenNameExtractor = screenNameExtractor
    }

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        if (activityLifecycle != null) {
            return
        }

        val app = (context as? Application) ?: (context.applicationContext as? Application) ?: return
        check(::visibleScreenTracker.isInitialized) { "Services must be configured before installation" }

        startupTimer.start(openTelemetryRum.openTelemetry.getTracer(INSTRUMENTATION_SCOPE), openTelemetryRum.clock)
        startupLifecycle =
            startupTimer.createLifecycleCallback().apply {
                app.registerActivityLifecycleCallbacks(this)
            }
        activityLifecycle =
            buildActivityLifecycleTracer(openTelemetryRum.openTelemetry).apply {
                app.registerActivityLifecycleCallbacks(this)
            }
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val app = (context as? Application) ?: (context.applicationContext as? Application) ?: return
        if (startupLifecycle != null) {
            app.unregisterActivityLifecycleCallbacks(startupLifecycle)
            startupLifecycle = null
        }
        if (activityLifecycle != null) {
            app.unregisterActivityLifecycleCallbacks(activityLifecycle)
            activityLifecycle = null
        }
    }

    private fun buildActivityLifecycleTracer(openTelemetry: OpenTelemetry): DefaultingActivityLifecycleCallbacks {
        val delegateTracer: Tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE)
        val tracers =
            ActivityTracerCache(
                tracerCustomizer.invoke(delegateTracer),
                visibleScreenTracker,
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
