/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServicesTest {
    @Before
    @After
    fun resetSingleton() {
        Services.set(null)
    }

    @Test
    fun `verify that services are created`() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val services = Services(ctx)
        assertThat(services).isNotNull
        assertThat(services.appLifecycle).isNotNull
        assertThat(services.visibleScreenTracker).isNotNull

        // assert no exceptions thrown
        services.close()
    }

    @Test
    fun `get returns singleton instance`() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val first = Services.get(ctx)
        val second = Services.get(ctx)

        assertThat(first).isSameAs(second)
        first.close()
    }

    @Test
    fun `get unwraps application context to avoid retaining non-application context`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val nonAppContext = mockk<Context>()
        every { nonAppContext.applicationContext } returns app

        val services = Services.get(nonAppContext)
        assertThat(services).isNotNull
        assertThat(services.visibleScreenTracker).isNotNull
        services.close()
    }

    @Test
    fun `close resets singleton instance`() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val first = Services.get(ctx)
        first.close()

        val second = Services.get(ctx)
        assertThat(second).isNotSameAs(first)
        second.close()
    }

    @Test
    fun `close on non-singleton instance does not clear singleton`() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val singleton = Services.get(ctx)
        val other = Services(ctx)

        other.close()

        assertThat(Services.get(ctx)).isSameAs(singleton)
        singleton.close()
    }
}
