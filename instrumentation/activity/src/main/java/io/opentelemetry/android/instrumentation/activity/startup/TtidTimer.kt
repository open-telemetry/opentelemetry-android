/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.startup

import android.view.Choreographer
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Measures Time To Initial Display (TTID): the time from app startup until the first frame is
 * drawn after the initial activity resumes. Produced as a child span of the "AppStart" span so it
 * shares the same start timestamp but reflects when content actually became visible rather than
 * when [android.app.Activity.onResume] returned.
 *
 * A frame is confirmed drawn using the standard double [Choreographer.postFrameCallback] trick: a
 * callback posted immediately after resume can still fire for a frame that was already in flight,
 * so the first callback does nothing but schedule a second one, whose frameTimeNanos is used as
 * the "first frame drawn" timestamp.
 */
internal class TtidTimer(
    private val tracer: Tracer,
    private val choreographerProvider: () -> Choreographer = { Choreographer.getInstance() },
) {
    private val started = AtomicBoolean(false)

    @Volatile
    private var pendingCallback: Choreographer.FrameCallback? = null

    /** Starts timing TTID, parented to [parentSpan] and anchored to [startTimestampNanos]. */
    fun start(
        parentSpan: Span,
        startTimestampNanos: Long,
        anchoredClock: AnchoredClock,
    ) {
        if (!started.compareAndSet(false, true)) {
            return
        }
        val span =
            tracer
                .spanBuilder(SPAN_NAME)
                .setParent(parentSpan.storeInContext(Context.current()))
                .setStartTimestamp(startTimestampNanos, TimeUnit.NANOSECONDS)
                .startSpan()
        postDoubleFrameCallback { frameTimeNanos ->
            pendingCallback = null
            span.end(anchoredClock.toEpochNanos(frameTimeNanos), TimeUnit.NANOSECONDS)
        }
    }

    /** Cancels a pending frame callback if the first frame was never observed (e.g. on uninstall). */
    fun cancel() {
        val choreographer = choreographerProvider()
        pendingCallback?.let { choreographer.removeFrameCallback(it) }
        pendingCallback = null
    }

    private fun postDoubleFrameCallback(onSecondFrame: (Long) -> Unit) {
        val choreographer = choreographerProvider()
        val secondCallback =
            Choreographer.FrameCallback { frameTimeNanos -> onSecondFrame(frameTimeNanos) }
        val firstCallback =
            Choreographer.FrameCallback {
                pendingCallback = secondCallback
                choreographer.postFrameCallback(secondCallback)
            }
        pendingCallback = firstCallback
        choreographer.postFrameCallback(firstCallback)
    }

    internal companion object {
        const val SPAN_NAME: String = "AppStartDisplay"
    }
}
