/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import java.time.Duration

internal const val SLOW_THRESHOLD_MS = 16
internal const val FROZEN_THRESHOLD_MS = 700

/**
 * Entrypoint for installing the slow rendering detection instrumentation.
 */
@AutoService(AndroidInstrumentation::class)
class SlowRenderingInstrumentation : AndroidInstrumentation {
    internal var debugVerbose: Boolean = false
    internal var slowRenderingDetectionPollInterval: Duration = Duration.ofSeconds(1)

    @Volatile
    private var detector: SlowRenderListener? = null

    /**
     * Configures the rate at which frame render durations are polled.
     *
     * @param interval The period that should be used for polling.
     * @return `this`
     */
    fun setSlowRenderingDetectionPollInterval(interval: Duration): SlowRenderingInstrumentation {
        if (interval.toMillis() <= 0) {
            Log.e(
                RumConstants.OTEL_RUM_LOG_TAG,
                (
                    "Invalid slowRenderingDetectionPollInterval: " +
                        interval +
                        "; must be positive"
                ),
            )
            return this
        }
        this.slowRenderingDetectionPollInterval = interval
        return this
    }

    /**
     * Call this to enable verbose debug logging when slow renders are detected.
     */
    fun enableVerboseDebugLogging(): SlowRenderingInstrumentation {
        debugVerbose = true
        return this
    }

    override fun install(context: Context, openTelemetryRum: OpenTelemetryRum) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "Slow/frozen rendering detection is not supported on platforms older than Android N (SDK version 24).",
            )
            return
        }
        if (detector != null) {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "SlowRenderingInstrumentation skipping installation (detector already installed)",
            )
            return
        }

        val logger = openTelemetryRum.openTelemetry.logsBridge.get("app.jank")
        var jankReporter: JankReporter = EventJankReporter(logger, SLOW_THRESHOLD_MS / 1000.0, debugVerbose)
        jankReporter = jankReporter.combine(EventJankReporter(logger, FROZEN_THRESHOLD_MS / 1000.0, debugVerbose))

        detector = SlowRenderListener(jankReporter, slowRenderingDetectionPollInterval)

        (context as? Application)?.registerActivityLifecycleCallbacks(detector)
        detector?.start()
    }

    override fun uninstall(context: Context, openTelemetryRum: OpenTelemetryRum) {
        if (detector == null) {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "SlowRenderingInstrumentation skipping uninstall (detector is null)",
            )
            return
        }
        (context as? Application)?.unregisterActivityLifecycleCallbacks(detector)
        detector?.shutdown()
        detector = null
    }

    override val name: String = "slowrendering"
}
