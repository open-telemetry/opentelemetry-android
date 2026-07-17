/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// The event name emitted by the generated AppScreenViewEvent (semconv/model/android/events.yaml).
internal const val SCREEN_VIEW_EVENT_NAME = "app.screen.view"

@OptIn(IncubatingApi::class)
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
    fun `emits a screen-view event carrying the screen name`() {
        navigationEmitter.onScreenView("home")

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(1)
        assertThat(events[0])
            .hasEventName(SCREEN_VIEW_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey(map(APP_SCREEN_NAME)), "home"),
            )
    }

    @Test
    fun `resolves its logger from an OpenTelemetryRum instance`() {
        val rum =
            mockk<OpenTelemetryRum> {
                every { openTelemetry } returns openTelemetryRule.openTelemetry
            }

        NavigationEmitter(rum).onScreenView("home")

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(1)
        assertThat(events[0])
            .hasEventName(SCREEN_VIEW_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey(map(APP_SCREEN_NAME)), "home"),
            )
    }

    @Test
    fun `emits one event per destination change`() {
        navigationEmitter.onScreenView("home")
        navigationEmitter.onScreenView("cart")

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(2)
        assertThat(events[0]).hasEventName(SCREEN_VIEW_EVENT_NAME)
        assertThat(events[1])
            .hasEventName(SCREEN_VIEW_EVENT_NAME)
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey(map(APP_SCREEN_NAME)), "cart"),
            )
    }
}
