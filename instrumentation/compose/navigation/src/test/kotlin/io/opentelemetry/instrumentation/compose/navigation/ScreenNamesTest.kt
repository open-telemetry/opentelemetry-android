/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import androidx.navigation.NavDestination
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenNamesTest {
    @Test
    fun `prefers the route pattern`() {
        val destination =
            mockk<NavDestination> {
                every { route } returns "user/{id}"
                every { label } returns "User"
                every { navigatorName } returns "composable"
            }

        assertThat(defaultScreenName(destination, null)).isEqualTo("user/{id}")
    }

    @Test
    fun `falls back to the label when route is null`() {
        val destination =
            mockk<NavDestination> {
                every { route } returns null
                every { label } returns "User"
                every { navigatorName } returns "composable"
            }

        assertThat(defaultScreenName(destination, null)).isEqualTo("User")
    }

    @Test
    fun `falls back to the navigator name when route and label are null`() {
        val destination =
            mockk<NavDestination> {
                every { route } returns null
                every { label } returns null
                every { navigatorName } returns "composable"
            }

        assertThat(defaultScreenName(destination, null)).isEqualTo("composable")
    }
}
