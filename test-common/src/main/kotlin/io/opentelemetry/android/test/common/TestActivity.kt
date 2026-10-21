/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.test.common

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout

class TestActivity: Activity() {
    companion object {
        val TEST_VIEW_WIDTH = 500
        val TEST_VIEW_HEIGHT = 500



    }
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
