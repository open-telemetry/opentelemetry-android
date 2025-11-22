/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.os.Build
import android.util.Log
import com.google.auto.service.AutoService
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import java.time.Duration

/**
 * Entrypoint for installing the slow rendering detection instrumentation.
 */
@AutoService(AndroidInstrumentation::class)
class SlowRenderingInstrumentation : AndroidInstrumentation {
    internal var useDeprecatedSpan: Boolean = false
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

    /**
     * Reports jank by using a zero-duration span.
     */
    @Deprecated("Use the default event to report jank")
    fun enableDeprecatedZeroDurationSpan(): SlowRenderingInstrumentation {
        useDeprecatedSpan = true
        return this
    }

    override fun install(ctx: InstallationContext) {
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

        val logger = ctx.openTelemetry.logsBridge.get("app.jank")
        var jankReporter: JankReporter = EventJankReporter(logger, ctx.sessionProvider, SLOW_THRESHOLD_MS / 1000.0, debugVerbose)
        jankReporter = jankReporter.combine(EventJankReporter(logger, ctx.sessionProvider, FROZEN_THRESHOLD_MS / 1000.0, debugVerbose))

        if (useDeprecatedSpan) {
            val tracer = ctx.openTelemetry.getTracer("io.opentelemetry.slow-rendering")
            jankReporter = jankReporter.combine(SpanBasedJankReporter(tracer))
        }

        detector = SlowRenderListener(jankReporter, slowRenderingDetectionPollInterval)

        ctx.application?.registerActivityLifecycleCallbacks(detector)
        detector!!.start()
    }

    override fun uninstall(ctx: InstallationContext) {
        if (detector == null) {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "SlowRenderingInstrumentation skipping uninstall (detector is null)",
            )
            return
        }
        ctx.application?.unregisterActivityLifecycleCallbacks(detector)
        detector?.shutdown()
        detector = null
    }

    override val name: String = "slowrendering"
}
