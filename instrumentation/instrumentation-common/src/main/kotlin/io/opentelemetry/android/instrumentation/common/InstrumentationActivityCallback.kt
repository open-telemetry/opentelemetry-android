package io.opentelemetry.android.instrumentation.common

import android.app.Activity
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

class InstrumentationActivityCallback(
    private val toggleableTracker: ToggleableTracker,
) : DefaultingActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        toggleableTracker.startTracking(activity.window)
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        toggleableTracker.stopTracking()
    }
}
