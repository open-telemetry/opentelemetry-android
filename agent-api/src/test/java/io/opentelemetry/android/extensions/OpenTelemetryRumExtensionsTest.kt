/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.Clock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OpenTelemetryRumExtensionsTest {
    @Test
    fun `existing implementations inherit null lookup`() {
        val rum =
            object : OpenTelemetryRum {
                override val openTelemetry = OpenTelemetry.noop()
                override val sessionProvider = SessionProvider.getNoop()
                override val clock = Clock.getDefault()

                override fun emitEvent(
                    eventName: String,
                    body: String,
                    attributes: Attributes,
                ) = Unit

                override fun shutdown() = Unit
            }
        assertThat(rum.getExtension(ExampleApi::class.java)).isNull()
        assertThat(rum.extension<ExampleApi>()).isNull()
    }

    @Test
    fun `reified lookup delegates with the public type`() {
        val rum = mockk<OpenTelemetryRum>()
        val extension = mockk<ExampleApi>()
        every { rum.getExtension(ExampleApi::class.java) } returns extension
        assertThat(rum.extension<ExampleApi>()).isSameAs(extension)
    }

    private interface ExampleApi : ApiExtension
}
