/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment

import android.app.Application.ActivityLifecycleCallbacks
import android.os.Build
import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.common.Constants.INSTRUMENTATION_SCOPE
import io.opentelemetry.android.instrumentation.common.DefaultScreenNameExtractor
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.visiblescreen.fragments.RumFragmentActivityRegisterer
import io.opentelemetry.api.trace.Tracer

@AutoService(AndroidInstrumentation::class)
class FragmentLifecycleInstrumentation : AndroidInstrumentation {
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

    override fun install(ctx: InstallationContext) {
        activityLifecycleCallbacks =
            buildFragmentRegisterer(ctx).apply {
                ctx.application?.registerActivityLifecycleCallbacks(this)
            }
    }

    override fun uninstall(ctx: InstallationContext) {
        super.uninstall(ctx)
        ctx.application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        activityLifecycleCallbacks = null
    }

    private fun buildFragmentRegisterer(ctx: InstallationContext): ActivityLifecycleCallbacks {
        val visibleScreenService = Services.get(ctx.context).visibleScreenTracker
        val delegateTracer: Tracer = ctx.openTelemetry.getTracer(INSTRUMENTATION_SCOPE)
        val fragmentLifecycle =
            RumFragmentLifecycleCallbacks(
                tracerCustomizer.invoke(delegateTracer),
                visibleScreenService::previouslyVisibleScreen,
                screenNameExtractor,
            )
        return if (Build.VERSION.SDK_INT < 29) {
            RumFragmentActivityRegisterer.createPre29(fragmentLifecycle)
        } else {
            RumFragmentActivityRegisterer.create(fragmentLifecycle)
        }
    }
}
