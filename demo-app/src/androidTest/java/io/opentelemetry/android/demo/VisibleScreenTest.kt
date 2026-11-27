/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.demo.about.AboutActivity
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.trace.ReadableSpan
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisibleScreenTest {
    @Test
    fun spanHasScreenNameOfActivityWithoutFragments() {
        // Shows AppFeaturesFragment
        ActivityScenario.launch(AboutActivity::class.java).close()

        ActivityScenario.launch(OkHttpDemoActivity::class.java).use {
            val span = OtelDemoApplication.tracer("test")!!.spanBuilder("test").startSpan() as ReadableSpan
            assertEquals("OkHttpDemoActivity", span.getAttribute(stringKey("app.screen.name")))
        }
    }
}
