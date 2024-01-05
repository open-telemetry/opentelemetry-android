/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class DefaultExportSchedulerTest {
    private lateinit var scheduler: DefaultExportScheduler

    @BeforeEach
    fun setUp() {
        scheduler = DefaultExportScheduler()
    }

    @Test
    fun `Verify minimum delay`() {
        assertThat(scheduler.minimumDelayUntilNextRunInMillis()).isEqualTo(
            TimeUnit.SECONDS.toMillis(
                10,
            ),
        )
    }
}
