/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks
import java.time.Duration
import java.util.WeakHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@RequiresApi(api = Build.VERSION_CODES.N)
internal class SlowRenderListener(
    private val jankReporter: JankReporter,
    private val executorService: ScheduledExecutorService,
    private val frameMetricsHandler: Handler,
    private val pollInterval: Duration,
) : DefaultingActivityLifecycleCallbacks {
    private val lock = Any()

    @GuardedBy("lock")
    private val activities: MutableMap<Activity, PerActivityListener> = WeakHashMap()

    // Not Duration.toSeconds(), which is missing before Android 12 (API 31).
    private val pollIntervalSeconds: Double = pollInterval.toMillis() / 1000.0

    constructor(jankReporter: JankReporter, pollInterval: Duration) : this(
        jankReporter,
        Executors.newScheduledThreadPool(1),
        Handler(startFrameMetricsLoop()),
        pollInterval,
    )

    // the returned future is very unlikely to fail
    fun start() {
        executorService.scheduleWithFixedDelay(
            { this.reportSlowRenders() },
            pollInterval.toMillis(),
            pollInterval.toMillis(),
            TimeUnit.MILLISECONDS,
        )
    }

    fun shutdown() {
        executorService.shutdownNow()
        val activeEntries =
            synchronized(lock) {
                val entries = activities.toList()
                activities.clear()
                entries
            }
        for ((activity, listener) in activeEntries) {
            activity.window.removeOnFrameMetricsAvailableListener(listener)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        try {
            registerActivity(activity)
        } catch (e: Exception) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Exception while starting frame metrics collection", e)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        try {
            unregisterActivity(activity)
        } catch (e: Exception) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Exception while stopping frame metrics collection", e)
        }
    }

    private fun registerActivity(activity: Activity) {
        if (executorService.isShutdown) {
            return
        }
        val listener = PerActivityListener(activity)
        val added =
            synchronized(lock) {
                if (!activities.containsKey(activity)) {
                    activities[activity] = listener
                    true
                } else {
                    false
                }
            }
        if (added) {
            activity.window.addOnFrameMetricsAvailableListener(listener, frameMetricsHandler)
        }
    }

    private fun unregisterActivity(activity: Activity) {
        if (executorService.isShutdown) {
            return
        }
        val listener =
            synchronized(lock) {
                activities.remove(activity)
            }
        if (listener != null) {
            activity.window.removeOnFrameMetricsAvailableListener(listener)
            val durationToCountHistogram = listener.resetMetrics()
            jankReporter.reportSlow(durationToCountHistogram, pollIntervalSeconds, listener.getActivityName())
        }
    }

    private fun reportSlowRenders() {
        try {
            val activeListeners =
                synchronized(lock) {
                    activities.values.toList()
                }
            for (listener in activeListeners) {
                val durationToCountHistogram = listener.resetMetrics()
                jankReporter.reportSlow(durationToCountHistogram, pollIntervalSeconds, listener.getActivityName())
            }
        } catch (e: Exception) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Exception while processing frame metrics", e)
        }
    }

    companion object {
        private val frameMetricsThread = HandlerThread("FrameMetricsCollector")

        private fun startFrameMetricsLoop(): Looper {
            // just a precaution: this is supposed to be called only once, and the thread should always
            // be not started here
            if (!frameMetricsThread.isAlive) {
                frameMetricsThread.start()
            }
            return frameMetricsThread.looper
        }
    }
}
