/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Application
import android.os.Build
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer
import io.opentelemetry.android.instrumentation.common.Constants.INSTRUMENTATION_SCOPE
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks
import io.opentelemetry.api.trace.Tracer

class ActivityLifecycleInstrumentation : AndroidInstrumentation {
    private val startupTimer: AppStartupTimer by lazy { AppStartupTimer() }
    private val screenNameExtractor: ScreenNameExtractor by lazy { ScreenNameExtractor.DEFAULT }

    override fun apply(
        application: Application,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        application.registerActivityLifecycleCallbacks(startupTimer.createLifecycleCallback())
        application.registerActivityLifecycleCallbacks(buildActivityLifecycleTracer(openTelemetryRum))
    }

    private fun buildActivityLifecycleTracer(openTelemetryRum: OpenTelemetryRum): DefaultingActivityLifecycleCallbacks {
        val visibleScreenService = ServiceManager.get().getService(VisibleScreenService::class.java)
        val delegateTracer: Tracer =
            openTelemetryRum.openTelemetry
                .getTracer(INSTRUMENTATION_SCOPE)
        val tracers =
            ActivityTracerCache(
                delegateTracer,
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
