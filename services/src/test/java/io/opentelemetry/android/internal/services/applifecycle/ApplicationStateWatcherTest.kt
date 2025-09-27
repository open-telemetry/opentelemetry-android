/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import androidx.core.app.ComponentActivity
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ApplicationStateWatcherTest {
    @MockK
    private lateinit var activity: ComponentActivity

    @RelaxedMockK
    private lateinit var listener1: ApplicationStateListener

    @RelaxedMockK
    private lateinit var listener2: ApplicationStateListener

    private lateinit var underTest: ApplicationStateWatcher

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        underTest = ApplicationStateWatcher()
        underTest.registerListener(listener1)
        underTest.registerListener(listener2)
    }

    @Test
    fun appForegrounded() {
        underTest.onStart(activity)

        verifyOrder {
            listener1.onApplicationForegrounded()
            listener2.onApplicationForegrounded()
        }
    }

    @Test
    fun appBackgrounded() {
        underTest.onStart(activity)
        underTest.onStop(activity)

        verifyOrder {
            listener1.onApplicationForegrounded()
            listener2.onApplicationForegrounded()
            listener1.onApplicationBackgrounded()
            listener2.onApplicationBackgrounded()
        }
    }

    @Test
    fun closing() {
        underTest.close()

        underTest.onStart(activity)
        underTest.onStop(activity)

        verify { listener1 wasNot Called }
        verify { listener2 wasNot Called }
    }
}
