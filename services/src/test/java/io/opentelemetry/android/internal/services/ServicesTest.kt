/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServicesTest {
    @Test
    fun `verify that services are created`() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val services = Services(ctx)
        assertNotNull(services)
        assertNotNull(services.appLifecycle)
        assertNotNull(services.cacheStorage)
        assertNotNull(services.currentNetworkProvider)
        assertNotNull(services.visibleScreenTracker)
        assertNotNull(services.periodicWork)

        // assert no exceptions thrown
        services.close()
    }
}
