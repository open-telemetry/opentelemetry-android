/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import io.opentelemetry.android.internal.services.network.PollingPermissionGrantCheckTrigger.Companion.CHECK_INTERVAL_MS
import io.opentelemetry.android.internal.services.network.PollingPermissionGrantCheckTrigger.Companion.STARTUP_WINDOW_MS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PollingPermissionGrantCheckTriggerTest {
    private var now = 0L
    private var granted = false
    private var checks = 0

    private fun createTrigger() =
        PollingPermissionGrantCheckTrigger(
            isGranted = {
                checks++
                granted
            },
            clock = { now },
        )

    @Test
    fun `never reports a grant when the permission was granted at startup`() {
        granted = true
        val trigger = createTrigger()

        assertThat(trigger.consumeTrigger()).isFalse()
        assertThat(checks).isEqualTo(1)
    }

    @Test
    fun `reports a grant only once`() {
        val trigger = createTrigger()
        granted = true

        assertThat(trigger.consumeTrigger()).isTrue()
        assertThat(trigger.consumeTrigger()).isFalse()
    }

    @Test
    fun `checks on every call during the startup window`() {
        val trigger = createTrigger()

        repeat(3) { trigger.consumeTrigger() }

        assertThat(checks).isEqualTo(4) // once at creation, then once per call
    }

    @Test
    fun `checks at most once per interval after the startup window`() {
        val trigger = createTrigger()
        now = STARTUP_WINDOW_MS

        repeat(3) { trigger.consumeTrigger() }
        now += CHECK_INTERVAL_MS - 1
        trigger.consumeTrigger()
        assertThat(checks).isEqualTo(2)

        now += 1
        trigger.consumeTrigger()
        assertThat(checks).isEqualTo(3)
    }

    @Test
    fun `reports a grant made after the startup window at the next check`() {
        val trigger = createTrigger()
        now = STARTUP_WINDOW_MS
        assertThat(trigger.consumeTrigger()).isFalse()

        granted = true
        now += CHECK_INTERVAL_MS - 1
        assertThat(trigger.consumeTrigger()).isFalse()

        now += 1
        assertThat(trigger.consumeTrigger()).isTrue()
    }
}
