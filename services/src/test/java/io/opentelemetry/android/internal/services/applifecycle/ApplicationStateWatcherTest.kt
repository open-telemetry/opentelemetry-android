/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import androidx.core.app.ComponentActivity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class ApplicationStateWatcherTest {
    @Mock
    private lateinit var activity: ComponentActivity

    @Mock
    private lateinit var listener1: ApplicationStateListener

    @Mock
    private lateinit var listener2: ApplicationStateListener

    private lateinit var underTest: ApplicationStateWatcher

    @BeforeEach
    fun setUp() {
        underTest = ApplicationStateWatcher()
        underTest.registerListener(listener1)
        underTest.registerListener(listener2)
    }

    @Test
    fun appForegrounded() {
        underTest.onStart(activity)

        val io = Mockito.inOrder(listener1, listener2)
        io.verify(listener1).onApplicationForegrounded()
        io.verify(listener2).onApplicationForegrounded()
        io.verifyNoMoreInteractions()
    }

    @Test
    fun appBackgrounded() {
        underTest.onStart(activity)
        underTest.onStop(activity)

        val io = Mockito.inOrder(listener1, listener2)
        io.verify(listener1).onApplicationForegrounded()
        io.verify(listener2).onApplicationForegrounded()
        io.verify(listener1).onApplicationBackgrounded()
        io.verify(listener2).onApplicationBackgrounded()
        io.verifyNoMoreInteractions()
    }

    @Test
    fun closing() {
        underTest.close()

        underTest.onStart(activity)
        underTest.onStop(activity)

        Mockito.verifyNoInteractions(listener1, listener2)
    }
}
