/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class SemconvCompatTest {
    var compatState: Boolean = true

    @AfterEach
    fun setup() {
        compatState = SemconvCompat.useLatestExperimental
    }

    @AfterEach
    fun restore() {
        SemconvCompat.useLatestExperimental = compatState
    }

    @Test
    fun `test compat with legacy`() {
        SemconvCompat.useLatestExperimental = false
        assertThat(SemconvCompat.map("app.screen.name")).isEqualTo("screen.name")
        assertThat(SemconvCompat.map("app.crash")).isEqualTo("device.crash")
        assertThat(SemconvCompat.map("rando.semconv.thinger")).isEqualTo("rando.semconv.thinger")
    }

    @Test
    fun `test compat with latest experimental`() {
        SemconvCompat.useLatestExperimental = true
        assertThat(SemconvCompat.map("app.screen.name")).isEqualTo("app.screen.name")
        assertThat(SemconvCompat.map("app.crash")).isEqualTo("app.crash")
        assertThat(SemconvCompat.map("rando.semconv.thinger")).isEqualTo("rando.semconv.thinger")
    }
}
