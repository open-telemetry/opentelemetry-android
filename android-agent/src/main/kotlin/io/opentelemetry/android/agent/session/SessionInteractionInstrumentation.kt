/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.logging.Level
import java.util.logging.Logger

internal class SessionInteractionInstrumentation(
    private val recorder: SessionUserInteractionRecorder,
    private val lifecycle: AppLifecycle,
) : AndroidInstrumentation,
    DefaultingActivityLifecycleCallbacks,
    ApplicationStateListener {
    override val name: String = "session.interaction"
    private val callbacks = WeakHashMap<Window, WeakReference<InteractionCallback>>()

    @Volatile
    private var installed = false

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        if (context !is Application) return
        installed = true
        context.registerActivityLifecycleCallbacks(this)
        lifecycle.registerListener(this)
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        installed = false
        (context as? Application)?.unregisterActivityLifecycleCallbacks(this)
        lifecycle.unregisterListener(this)
        val clear =
            Runnable {
                callbacks.keys.toList().forEach(::removeCallback)
            }
        if (Looper.myLooper() == Looper.getMainLooper()) clear.run() else Handler(Looper.getMainLooper()).post(clear)
    }

    override fun onApplicationForegrounded() {
        if (installed) recordInteraction()
    }

    override fun onApplicationBackgrounded() {}

    override fun onActivityResumed(activity: Activity) {
        if (!installed) return
        val window = activity.window
        val previous = callbacks[window]?.get()
        val callback =
            if (window.callback === previous) {
                checkNotNull(previous)
            } else {
                previous?.active = false
                InteractionCallback(window.callback).also {
                    window.callback = it
                    callbacks[window] = WeakReference(it)
                }
            }
        callback.active = true
    }

    override fun onActivityPaused(activity: Activity) {
        callbacks[activity.window]?.get()?.active = false
    }

    override fun onActivityDestroyed(activity: Activity) {
        removeCallback(activity.window)
    }

    private fun removeCallback(window: Window) {
        val callback = callbacks.remove(window)?.get() ?: return
        callback.active = false
        if (window.callback === callback) window.callback = callback.delegate
    }

    private fun recordInteraction() {
        try {
            recorder.recordUserInteraction()
        } catch (failure: Exception) {
            logger.log(Level.WARNING, "Unable to record session interaction", failure)
        }
    }

    private inner class InteractionCallback(
        val delegate: Window.Callback,
    ) : Window.Callback by delegate {
        var active = false

        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            val isTouchActivity = event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE
            if (installed && active && isTouchActivity) {
                recordInteraction()
            }
            return delegate.dispatchTouchEvent(event)
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (installed && active && event.action == KeyEvent.ACTION_DOWN) recordInteraction()
            return delegate.dispatchKeyEvent(event)
        }

        override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
            if (installed && active && event.actionMasked == MotionEvent.ACTION_SCROLL) recordInteraction()
            return delegate.dispatchGenericMotionEvent(event)
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger(SessionInteractionInstrumentation::class.java.name)
    }
}
