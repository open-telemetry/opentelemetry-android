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
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.OpenTelemetry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class WithOpenTelemetryComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val rum =
        mockk<OpenTelemetryRum> {
            every { openTelemetry } returns OpenTelemetry.noop()
        }

    @Test
    fun `rememberObservedNavController resolves a screen name on navigation`() {
        val seen = mutableListOf<String>()
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController =
                rememberObservedNavController(rum, screenName = { destination, arguments ->
                    defaultScreenName(destination, arguments).also { seen += it }
                })
            NavHost(navController, startDestination = "a") {
                composable("a") {}
                composable("b") {}
            }
        }

        composeRule.runOnIdle { navController.navigate("b") }
        composeRule.waitForIdle()

        assertThat(seen).isNotEmpty()
        assertThat(seen.last()).isEqualTo("b")
    }

    @Test
    fun `withOpenTelemetry uses the latest screenName after recomposition`() {
        var name by mutableStateOf("first")
        val seen = mutableListOf<String>()
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController =
                rememberNavController().withOpenTelemetry(rum) { _, _ ->
                    name.also { seen += it }
                }
            NavHost(navController, startDestination = "a") {
                composable("a") {}
                composable("b") {}
            }
        }

        composeRule.runOnIdle { navController.navigate("b") }
        composeRule.runOnIdle { name = "second" }
        composeRule.runOnIdle { navController.navigate("a") }

        assertThat(seen).containsExactly("first", "first", "second")
    }

    @Test
    fun `removing the instrumentation stops resolving screen names on later navigation`() {
        var instrumented by mutableStateOf(true)
        val seen = mutableListOf<String>()
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            if (instrumented) {
                navController.withOpenTelemetry(rum) { destination, arguments ->
                    defaultScreenName(destination, arguments).also { seen += it }
                }
            }
            NavHost(navController, startDestination = "a") {
                composable("a") {}
                composable("b") {}
            }
        }

        composeRule.runOnIdle { navController.navigate("b") }
        composeRule.waitForIdle()
        val countWhileInstrumented = seen.size

        // Remove the instrumenting composable from the tree; onDispose should remove the listener.
        composeRule.runOnIdle { instrumented = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { navController.navigate("a") }
        composeRule.waitForIdle()

        assertThat(seen).hasSize(countWhileInstrumented)
    }
}
