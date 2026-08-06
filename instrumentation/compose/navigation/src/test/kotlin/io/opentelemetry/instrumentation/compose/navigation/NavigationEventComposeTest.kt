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
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * End-to-end coverage: drives a real [NavHostController] through a composition and asserts on the
 * log records that come out of the SDK, rather than stubbing the controller or the emitter. This is
 * the only place the listener -> emitter -> logger chain is exercised as a whole.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class NavigationEventComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var otel: OpenTelemetryRule
    private lateinit var rum: OpenTelemetryRum

    @Before
    fun setup() {
        otel = OpenTelemetryRule.create()
        rum =
            mockk<OpenTelemetryRum> {
                every { openTelemetry } returns otel.openTelemetry
            }
    }

    private fun destinationNames(): List<String?> = otel.logRecords.map { it.attributes[stringKey(DESTINATION_NAME_KEY)] }

    @Test
    fun `emits an event for the start destination when the listener attaches`() {
        composeRule.setContent {
            val navController = rememberObservedNavController(rum)
            NavHost(navController, startDestination = "a") {
                composable("a") {}
                composable("b") {}
            }
        }
        composeRule.waitForIdle()

        assertThat(otel.logRecords).hasSize(1)
        assertThat(otel.logRecords[0])
            .hasEventName(NAVIGATION_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey(DESTINATION_NAME_KEY), "a"),
            )
    }

    @Test
    fun `emits an event through the RUM logger on navigation`() {
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

        assertThat(destinationNames()).containsExactly("a", "b")
        assertThat(otel.logRecords.last())
            .hasEventName(NAVIGATION_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey(DESTINATION_NAME_KEY), "b"),
            )
    }

    @Test
    fun `emits an event when only the arguments change`() {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberObservedNavController(rum)
            NavHost(navController, startDestination = "user/1") {
                composable("user/{id}") {}
            }
        }

        composeRule.runOnIdle { navController.navigate("user/2") }
        composeRule.waitForIdle()

        // A new back stack entry, so a real navigation - but the default resolver reports the route
        // pattern, so both events carry the same name.
        assertThat(destinationNames()).containsExactly("user/{id}", "user/{id}")
    }

    @Test
    fun `screenName reading arguments tells navigations apart but sees none on attach`() {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController =
                rememberObservedNavController(rum, screenName = { _, arguments ->
                    arguments?.getString("id")?.let { "user/$it" } ?: "user/unknown"
                })
            NavHost(navController, startDestination = "user/1") {
                composable("user/{id}") {}
            }
        }

        composeRule.runOnIdle { navController.navigate("user/2") }
        composeRule.runOnIdle { navController.navigate("user/3") }
        composeRule.waitForIdle()

        // The attach replay carries a null arguments bundle even for a parameterised start
        // destination, so `id` is unavailable there; real navigations do supply it.
        assertThat(destinationNames()).containsExactly("user/unknown", "user/2", "user/3")
    }

    @Test
    fun `re-attaching emits again without a navigation`() {
        var instrumented by mutableStateOf(true)
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            if (instrumented) {
                navController.withOpenTelemetry(rum)
            }
            NavHost(navController, startDestination = "a") {
                composable("a") {}
                composable("b") {}
            }
        }
        composeRule.waitForIdle()
        val afterFirstAttach = otel.logRecords.size

        composeRule.runOnIdle { instrumented = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { instrumented = true }
        composeRule.waitForIdle()

        assertThat(otel.logRecords).hasSize(afterFirstAttach + 1)
        assertThat(otel.logRecords.last())
            .hasEventName(NAVIGATION_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey(DESTINATION_NAME_KEY), "a"),
            )
    }
}
