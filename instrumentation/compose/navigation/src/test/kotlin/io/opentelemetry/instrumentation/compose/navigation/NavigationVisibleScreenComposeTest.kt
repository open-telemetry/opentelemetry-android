/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Covers the [VisibleScreenTracker] side of the instrumentation: the resolved screen name is
 * reported on every destination change and cleared when the controller leaves the composition.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class NavigationVisibleScreenComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var rum: OpenTelemetryRum
    private lateinit var visibleScreenTracker: VisibleScreenTracker

    @Before
    fun setup() {
        val otel = OpenTelemetryRule.create()
        rum =
            mockk<OpenTelemetryRum> {
                every { openTelemetry } returns otel.openTelemetry
            }
        visibleScreenTracker = mockk(relaxed = true)
        Services.set(
            mockk<Services> {
                every { this@mockk.visibleScreenTracker } returns this@NavigationVisibleScreenComposeTest.visibleScreenTracker
            },
        )
    }

    @After
    fun tearDown() {
        Services.set(null)
    }

    @Test
    fun `reports the resolved screen name on attach and on each navigation`() {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberObservedNavController(rum)
            NavHost(navController, startDestination = "a") {
                composable("a") {}
                composable("b") {}
            }
        }

        composeRule.runOnIdle { navController.navigate("b") }
        composeRule.waitForIdle()

        verifyOrder {
            visibleScreenTracker.navigationDestinationChanged("a")
            visibleScreenTracker.navigationDestinationChanged("b")
        }
    }

    @Test
    fun `reports the route pattern rather than the filled in arguments`() {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberObservedNavController(rum)
            NavHost(navController, startDestination = "user/1") {
                composable("user/{id}") {}
            }
        }

        composeRule.runOnIdle { navController.navigate("user/2") }
        composeRule.waitForIdle()

        verify(exactly = 2) { visibleScreenTracker.navigationDestinationChanged("user/{id}") }
        verify(exactly = 0) { visibleScreenTracker.navigationDestinationChanged("user/2") }
    }

    @Test
    fun `clears the destination when the controller leaves the composition`() {
        var instrumented by mutableStateOf(true)
        composeRule.setContent {
            val navController = rememberNavController()
            if (instrumented) {
                navController.withOpenTelemetry(rum)
            }
            NavHost(navController, startDestination = "a") {
                composable("a") {}
            }
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle { instrumented = false }
        composeRule.waitForIdle()

        verify(exactly = 1) { visibleScreenTracker.navigationDestinationCleared("a") }
    }

    @Test
    fun `a dispose and re-attach cycle clears and then replays the destination`() {
        var instrumented by mutableStateOf(true)
        composeRule.setContent {
            val navController = rememberNavController()
            if (instrumented) {
                navController.withOpenTelemetry(rum)
            }
            NavHost(navController, startDestination = "a") {
                composable("a") {}
            }
        }
        composeRule.waitForIdle()

        // Approximates a configuration change: the effect is disposed and then re-registered,
        // and registering replays the destination that is already showing.
        composeRule.runOnIdle { instrumented = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { instrumented = true }
        composeRule.waitForIdle()

        verifyOrder {
            visibleScreenTracker.navigationDestinationChanged("a")
            visibleScreenTracker.navigationDestinationCleared("a")
            visibleScreenTracker.navigationDestinationChanged("a")
        }
    }
}
