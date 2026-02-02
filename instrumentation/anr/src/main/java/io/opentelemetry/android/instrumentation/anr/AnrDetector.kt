/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import android.os.Handler
import android.os.Looper
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.api.OpenTelemetry
import java.util.concurrent.ScheduledExecutorService

/** Entrypoint for installing the ANR (application not responding) detection instrumentation.  */
internal class AnrDetector(
    private val additionalExtractors: List<EventAttributesExtractor<Array<StackTraceElement>>>,
    private val mainLooper: Looper,
    private val scheduler: ScheduledExecutorService,
    private val appLifecycle: AppLifecycle,
    private val openTelemetry: OpenTelemetry,
) {
    /**
     * Starts the ANR detection instrumentation.
     *
     * When the main thread is unresponsive for 5 seconds or more, an event including the main
     * thread's stack trace will be reported to the RUM system.
     */
    fun start() {
        val uiHandler = Handler(mainLooper)
        val anrLogger = openTelemetry.logsBridge.get("io.opentelemetry.anr")
        val anrWatcher = AnrWatcher(uiHandler, mainLooper.thread, anrLogger, additionalExtractors)

        val listener = AnrDetectorToggler(anrWatcher, scheduler)
        // call it manually the first time to enable the ANR detection
        listener.onApplicationForegrounded()

        appLifecycle.registerListener(listener)
    }
}
