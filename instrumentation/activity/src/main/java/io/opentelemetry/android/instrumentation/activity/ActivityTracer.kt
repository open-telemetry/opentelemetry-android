/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import io.opentelemetry.android.common.internal.SemconvCompat.Companion.map
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer
import io.opentelemetry.android.instrumentation.activity.startup.TtidTimer
import io.opentelemetry.android.instrumentation.common.ActiveSpan
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi

internal class ActivityTracer(
    activity: Activity,
    private val activeSpan: ActiveSpan,
    private val tracer: Tracer,
    private val appStartupTimer: AppStartupTimer,
    private val ttidTimer: TtidTimer,
    screenName: String? = null,
    private var initialAppActivity: String? = null,
) {
    private val screenName: String = screenName ?: "unknown_screen"
    private val activityName = activity.javaClass.simpleName

    fun startSpanIfNoneInProgress(spanName: String): ActivityTracer {
        if (activeSpan.spanInProgress()) {
            return this
        }
        activeSpan.startSpan { createSpan(spanName) }
        return this
    }

    fun startActivityCreation(): ActivityTracer {
        activeSpan.startSpan { this.makeCreationSpan() }
        return this
    }

    private fun makeCreationSpan(): Span {
        // If the application has never loaded an activity, or this is the initial activity getting
        // re-created,
        // we name this span specially to show that it's the application starting up. Otherwise, use
        // the activity class name as the base of the span name.
        val isColdStart = initialAppActivity == null
        if (isColdStart) {
            return createSpanWithParent("Created", appStartupTimer.startupSpan)
        }
        if (activityName == initialAppActivity) {
            return createAppStartSpan("warm")
        }
        return createSpan("Created")
    }

    fun initiateRestartSpanIfNecessary(multiActivityApp: Boolean): ActivityTracer {
        if (activeSpan.spanInProgress()) {
            return this
        }
        activeSpan.startSpan { makeRestartSpan(multiActivityApp) }
        return this
    }

    private fun makeRestartSpan(multiActivityApp: Boolean): Span {
        // restarting the first activity is a "hot" AppStart
        // Note: in a multi-activity application, navigating back to the first activity can trigger
        // this, so it would not be ideal to call it an AppStart.
        if (!multiActivityApp && activityName == initialAppActivity) {
            return createAppStartSpan("hot")
        }
        return createSpan("Restarted")
    }

    private fun createAppStartSpan(startType: String): Span {
        val span = createSpan(APP_START_SPAN_NAME)
        span.setAttribute(START_TYPE_KEY, startType)
        return span
    }

    private fun createSpan(spanName: String): Span = createSpanWithParent(spanName, null)

    @OptIn(IncubatingApi::class)
    private fun createSpanWithParent(
        spanName: String,
        parentSpan: Span?,
    ): Span {
        val spanBuilder = tracer.spanBuilder(spanName).setAttribute(ACTIVITY_NAME_KEY, activityName)
        if (parentSpan != null) {
            spanBuilder.setParent(parentSpan.storeInContext(Context.current()))
        }
        val span = spanBuilder.startSpan()
        // do this after the span is started, so we can override the default screen.name set by the
        // RumAttributeAppender.
        span.setAttribute(map(APP_SCREEN_NAME), screenName)
        return span
    }

    fun endSpanForActivityResumed() {
        if (initialAppActivity == null) {
            initialAppActivity = activityName
            appStartupTimer.startupSpan?.let { startupSpan ->
                ttidTimer.start(startupSpan, appStartupTimer.startTimestampNanos, appStartupTimer.anchoredClock)
            }
        }
        endActiveSpan()
    }

    fun endActiveSpan() {
        // If we happen to be in app startup, make sure this ends it. It's harmless if we're already
        // out of the startup phase.
        appStartupTimer.end()
        activeSpan.endActiveSpan()
    }

    fun addPreviousScreenAttribute(): ActivityTracer {
        activeSpan.addPreviousScreenAttribute(activityName)
        return this
    }

    fun addEvent(eventName: String): ActivityTracer {
        activeSpan.addEvent(eventName)
        return this
    }

    internal companion object {
        val ACTIVITY_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("activity.name")
        const val APP_START_SPAN_NAME: String = "AppStart"
        val START_TYPE_KEY: AttributeKey<String> = AttributeKey.stringKey("start.type")
    }
}
