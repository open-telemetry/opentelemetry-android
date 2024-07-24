/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.initialization

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class InitializationEventsTest {
    @AfterEach
    fun tearDown() {
        InitializationEvents.resetForTest()
    }

    @Test
    fun `Verify service loading`() {
        val initializationEvents = InitializationEvents.get()
        assertThat(initializationEvents).isInstanceOf(TestInitializationEvents::class.java)
    }

    @Test
    fun `Verify setting instance once`() {
        val initializationEvents = mockk<InitializationEvents>()
        InitializationEvents.set(initializationEvents)

        assertThat(InitializationEvents.get()).isEqualTo(initializationEvents)
    }
}
