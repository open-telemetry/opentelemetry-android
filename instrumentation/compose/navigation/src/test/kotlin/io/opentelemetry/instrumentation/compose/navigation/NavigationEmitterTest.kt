/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Emitted by the generated AppNavigationEvent (semconv/model/android/events.yaml).
internal const val NAVIGATION_EVENT_NAME = "app.navigation.complete"
internal const val DESTINATION_NAME_KEY = "app.navigation.destination.name"

@RunWith(AndroidJUnit4::class)
class NavigationEmitterTest {
    private lateinit var openTelemetryRule: OpenTelemetryRule
    private lateinit var navigationEmitter: NavigationEmitter

    @Before
    fun setup() {
        openTelemetryRule = OpenTelemetryRule.create()
        navigationEmitter =
            NavigationEmitter(
                openTelemetryRule.openTelemetry.logsBridge
                    .loggerBuilder(INSTRUMENTATION_SCOPE_NAME)
                    .build(),
            )
    }

    @Test
    fun `emits a navigation event carrying the destination name`() {
        navigationEmitter.onNavigation("home")

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(1)
        assertThat(events[0])
            .hasEventName(NAVIGATION_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey(DESTINATION_NAME_KEY), "home"),
            )
    }

    @Test
    fun `resolves its logger from an OpenTelemetryRum instance`() {
        val rum =
            mockk<OpenTelemetryRum> {
                every { openTelemetry } returns openTelemetryRule.openTelemetry
            }

        NavigationEmitter(rum).onNavigation("home")

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(1)
        assertThat(events[0])
            .hasEventName(NAVIGATION_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey(DESTINATION_NAME_KEY), "home"),
            )
    }

    @Test
    fun `emits one event per onNavigation invocation`() {
        navigationEmitter.onNavigation("home")
        navigationEmitter.onNavigation("cart")

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)
        assertThat(events[0])
            .hasEventName(NAVIGATION_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey(DESTINATION_NAME_KEY), "home"),
            )
        assertThat(events[1])
            .hasEventName(NAVIGATION_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey(DESTINATION_NAME_KEY), "cart"),
            )
    }
}
