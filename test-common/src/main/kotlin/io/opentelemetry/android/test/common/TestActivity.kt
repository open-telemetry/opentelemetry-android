/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.test.common

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout

class TestActivity : Activity() {
    companion object {
        private const val TEST_VIEW_WIDTH = 500
        private const val TEST_VIEW_HEIGHT = 500
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewGroup = LinearLayout(this)
        val view = View(this)
        view.id = 10012
        view.layoutParams = LinearLayout.LayoutParams(TEST_VIEW_WIDTH, TEST_VIEW_HEIGHT)
        viewGroup.addView(view)
        setContentView(viewGroup, LinearLayout.LayoutParams(TEST_VIEW_WIDTH, TEST_VIEW_HEIGHT))
    }
}
