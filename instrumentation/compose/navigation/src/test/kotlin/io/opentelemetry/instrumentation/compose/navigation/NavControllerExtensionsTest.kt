/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
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

@OptIn(IncubatingApi::class)
@RunWith(AndroidJUnit4::class)
class NavControllerExtensionsTest {
    private lateinit var openTelemetryRule: OpenTelemetryRule
    private lateinit var emitter: NavigationEmitter

    @Before
    fun setup() {
        openTelemetryRule = OpenTelemetryRule.create()
        emitter =
            NavigationEmitter(
                openTelemetryRule.openTelemetry.logsBridge
                    .loggerBuilder(INSTRUMENTATION_SCOPE_NAME)
                    .build(),
            )
    }

    @Test
    fun `emits a screen-view event on each destination change`() {
        val controller = mockk<NavController>(relaxed = true)
        val listener = controller.attachOpenTelemetry({ emitter }, { { _, _ -> "home" } })

        listener.onDestinationChanged(controller, mockk<NavDestination>(), null)

        val events = openTelemetryRule.logRecords
        assertThat(events).hasSize(1)
        assertThat(events[0])
            .hasEventName(SCREEN_VIEW_EVENT_NAME)
            .hasAttributesSatisfyingExactly(equalTo(stringKey(map(APP_SCREEN_NAME)), "home"))
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

        val names =
            openTelemetryRule.logRecords.map { record ->
                record.attributes.get(stringKey(map(APP_SCREEN_NAME)))
            }
        assertThat(names).containsExactly("first", "second")
    }
}
