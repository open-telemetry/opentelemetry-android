/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment

import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Build
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.common.Constants.INSTRUMENTATION_SCOPE
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService
import io.opentelemetry.android.internal.services.visiblescreen.fragments.RumFragmentActivityRegisterer
import io.opentelemetry.api.trace.Tracer

class FragmentLifecycleInstrumentation : AndroidInstrumentation {
    private val screenNameExtractor by lazy { ScreenNameExtractor.DEFAULT }

    override fun apply(
        application: Application,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        application.registerActivityLifecycleCallbacks(buildFragmentRegisterer(openTelemetryRum))
    }

    private fun buildFragmentRegisterer(openTelemetryRum: OpenTelemetryRum): ActivityLifecycleCallbacks {
        val visibleScreenService = ServiceManager.get().getService(VisibleScreenService::class.java)
        val delegateTracer: Tracer = openTelemetryRum.openTelemetry.getTracer(INSTRUMENTATION_SCOPE)
        val fragmentLifecycle =
            RumFragmentLifecycleCallbacks(
                delegateTracer,
                visibleScreenService::getPreviouslyVisibleScreen,
                screenNameExtractor,
            )
        return if (Build.VERSION.SDK_INT < 29) {
            RumFragmentActivityRegisterer.createPre29(fragmentLifecycle)
        } else {
            RumFragmentActivityRegisterer.create(fragmentLifecycle)
        }
    }
}
