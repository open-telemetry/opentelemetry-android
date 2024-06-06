/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.config.OtelRumConfig
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
internal class AgentInitTest {
    @Config(sdk = [LOLLIPOP, UPSIDE_DOWN_CAKE])
    @Test
    fun startOpenTelemetryRumInAndroid() {
        val application = getApplicationContext<Application>()
        val otelRum =
            OpenTelemetryRum.builder(
                application,
                OtelRumConfig(),
            ).build()
        assertNotNull(otelRum)
    }
}
