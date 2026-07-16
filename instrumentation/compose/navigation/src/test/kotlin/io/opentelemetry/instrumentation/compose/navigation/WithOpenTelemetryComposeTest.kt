/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.common.internal.SemconvCompat.Companion.map
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@OptIn(IncubatingApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class WithOpenTelemetryComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val openTelemetryRule = OpenTelemetryRule.create()
    private val rum =
        mockk<OpenTelemetryRum> {
            every { openTelemetry } returns openTelemetryRule.openTelemetry
        }

    @Test
    fun `rememberObservedNavController emits a screen-view event on navigation`() {
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

        val screenViews = openTelemetryRule.logRecords.filter { it.eventName == SCREEN_VIEW_EVENT_NAME }
        assertThat(screenViews).isNotEmpty()
        assertThat(screenViews.last())
            .hasAttributesSatisfying(equalTo(stringKey(map(APP_SCREEN_NAME)), "b"))
    }

    @Test
    fun `withOpenTelemetry uses the latest screenName after recomposition`() {
        var name by mutableStateOf("first")
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController().withOpenTelemetry(rum) { _, _ -> name }
            NavHost(navController, startDestination = "a") {
                composable("a") {}
                composable("b") {}
            }
        }

        composeRule.runOnIdle { navController.navigate("b") }
        composeRule.runOnIdle { name = "second" }
        composeRule.runOnIdle { navController.navigate("a") }

        val names =
            openTelemetryRule.logRecords.map { record ->
                record.attributes.get(stringKey(map(APP_SCREEN_NAME)))
            }
        assertThat(names).containsExactly("first", "first", "second")
    }
}
