/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity.draw

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver

internal object FirstDrawListener {
    fun registerFirstDraw(
        activity: Activity,
        drawDoneCallback: (View) -> Unit,
    ) {
        val window = activity.window

        // Wait until the decorView is created until we actually use it
        window.onDecorViewReady {
            val decorView = window.peekDecorView()
            val versionsBeforeBugFix = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
            val decorViewAttached = decorView.viewTreeObserver.isAlive && decorView.isAttachedToWindow

            // Before API version 26, draw listeners were not merged back into the real view tree observer
            // Workaround is to wait until the view is attached before registering draw listeners
            // Source: https://android.googlesource.com/platform/frameworks/base/+/9f8ec54244a5e0343b9748db3329733f259604f3
            if (versionsBeforeBugFix && !decorViewAttached) {
                decorView.addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {
                            decorView.viewTreeObserver.addOnDrawListener(NextDrawListener(decorView, drawDoneCallback))
                            decorView.removeOnAttachStateChangeListener(this)
                        }

                        override fun onViewDetachedFromWindow(v: View) = Unit
                    },
                )
            } else {
                decorView.viewTreeObserver.addOnDrawListener(NextDrawListener(decorView, drawDoneCallback))
            }
        }
    }

    /**
     * ViewTreeObserver.removeOnDrawListener() cannot be called from the onDraw() callback,
     * so remove it in next draw.
     */
    internal class NextDrawListener(
        val view: View,
        val drawDoneCallback: (View) -> Unit,
        val handler: Handler = Handler(Looper.getMainLooper()),
    ) : ViewTreeObserver.OnDrawListener {
        var invoked = false

        override fun onDraw() {
            if (!invoked) {
                invoked = true
                drawDoneCallback(view)
                handler.post {
                    if (view.viewTreeObserver.isAlive) {
                        view.viewTreeObserver.removeOnDrawListener(this)
                    }
                }
            }
        }
    }
}
