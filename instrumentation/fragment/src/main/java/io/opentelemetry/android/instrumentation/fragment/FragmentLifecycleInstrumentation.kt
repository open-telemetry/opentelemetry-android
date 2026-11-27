/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment

import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Build
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.common.Constants.INSTRUMENTATION_SCOPE
import io.opentelemetry.android.instrumentation.common.DefaultScreenNameExtractor
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.internal.services.ServicesAwareInstrumentation
import io.opentelemetry.android.internal.services.ServicesFactory
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.android.internal.services.visiblescreen.fragments.RumFragmentActivityRegisterer
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer

@AutoService(AndroidInstrumentation::class)
class FragmentLifecycleInstrumentation :
    AndroidInstrumentation,
    ServicesAwareInstrumentation {
    private lateinit var visibleScreenTracker: VisibleScreenTracker
    private var screenNameExtractor: ScreenNameExtractor = DefaultScreenNameExtractor
    private var tracerCustomizer: (Tracer) -> Tracer = { it }
    private var activityLifecycleCallbacks: ActivityLifecycleCallbacks? = null

    fun setTracerCustomizer(customizer: (Tracer) -> Tracer) {
        tracerCustomizer = customizer
    }

    fun setScreenNameExtractor(screenNameExtractor: ScreenNameExtractor) {
        this.screenNameExtractor = screenNameExtractor
    }

    override val name: String = "fragment"

    override fun configureServices(services: ServicesFactory) {
        visibleScreenTracker = services.visibleScreenTracker
    }

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val app = context as? Application ?: return
        check(::visibleScreenTracker.isInitialized) { "Services must be configured before installation" }
        val callbacks = buildFragmentRegisterer(openTelemetryRum.openTelemetry)
        app.registerActivityLifecycleCallbacks(callbacks)
        activityLifecycleCallbacks = callbacks
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        val callbacks = activityLifecycleCallbacks ?: return
        (context as? Application)?.unregisterActivityLifecycleCallbacks(callbacks)
        activityLifecycleCallbacks = null
    }

    private fun buildFragmentRegisterer(openTelemetry: OpenTelemetry): ActivityLifecycleCallbacks {
        val delegateTracer: Tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE)
        val fragmentLifecycle =
            RumFragmentLifecycleCallbacks(
                tracerCustomizer.invoke(delegateTracer),
                visibleScreenTracker::previouslyVisibleScreen,
                screenNameExtractor,
            )
        return if (Build.VERSION.SDK_INT < 29) {
            RumFragmentActivityRegisterer.createPre29(fragmentLifecycle)
        } else {
            RumFragmentActivityRegisterer.create(fragmentLifecycle)
        }
    }
}
