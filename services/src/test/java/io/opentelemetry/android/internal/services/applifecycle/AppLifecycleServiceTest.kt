/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import androidx.lifecycle.Lifecycle
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppLifecycleServiceTest {
    @MockK
    private lateinit var applicationStateWatcher: ApplicationStateWatcher

    @MockK
    private lateinit var lifecycle: Lifecycle

    private lateinit var lifecycleService: AppLifecycleService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        lifecycleService = AppLifecycleService(applicationStateWatcher, lifecycle)
    }

    @Test
    fun `Registering listener`() {
        val listener = mockk<ApplicationStateListener>()
        every { applicationStateWatcher.registerListener(any()) } just Runs

        lifecycleService.registerListener(listener)

        verify { applicationStateWatcher.registerListener(listener) }
    }

    @Test
    fun `Starting to observe app's lifecycle`() {
        every { lifecycle.addObserver(any()) } just Runs

        lifecycleService.start()

        verify { lifecycle.addObserver(applicationStateWatcher) }
    }
}
