package io.opentelemetry.android.instrumentation.view.scale

import android.app.Activity
import io.opentelemetry.android.instrumentation.internal.InternalViewApi
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

@InternalViewApi
internal class ViewScaleActivityCallback(
    private val viewScaleEventGenerator: ViewScaleEventGenerator,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        viewScaleEventGenerator.startTracking(activity.window)
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        viewScaleEventGenerator.stopTracking()
    }
}
