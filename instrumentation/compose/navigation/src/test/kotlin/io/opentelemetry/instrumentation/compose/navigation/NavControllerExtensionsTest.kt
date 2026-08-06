/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavControllerExtensionsTest {
    private val emitter = mockk<NavigationEmitter>(relaxed = true)

    @Test
    fun `notifies the emitter on each destination change`() {
        val controller = mockk<NavController>(relaxed = true)
        val listener = controller.attachOpenTelemetry({ emitter }, { { _, _ -> "home" } })

        listener.onDestinationChanged(controller, mockk<NavDestination>(), null)

        verify(exactly = 1) { emitter.onScreenView("home") }
    }

    @Test
    fun `listener uses the latest screenName supplied at fire time`() {
        val controller = mockk<NavController>(relaxed = true)
        var name = "first"
        val listener = controller.attachOpenTelemetry({ emitter }, { { _, _ -> name } })

        val destination = mockk<NavDestination>()
        listener.onDestinationChanged(controller, destination, null)
        name = "second"
        listener.onDestinationChanged(controller, destination, null)

        verifyOrder {
            emitter.onScreenView("first")
            emitter.onScreenView("second")
        }
    }
}
