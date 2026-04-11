package io.opentelemetry.android.doubletap

import android.app.Activity
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

internal class DoubleTapActivityCallback(
    private val doubleTapEventGenerator: DoubleTapEventGenerator
): DefaultingActivityLifecycleCallbacks {

    override fun onActivityResumed(activity: Activity){
        super.onActivityResumed(activity)
        doubleTapEventGenerator.startTracking(activity.window)
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        doubleTapEventGenerator.stopTracking()
    }
}
