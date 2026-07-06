/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering

import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DiskBufferingConfigTest {
    @Test
    fun `defaults to a ten second export period`() {
        assertThat(DiskBufferingConfig.create().exportPeriodMillis)
            .isEqualTo(TimeUnit.SECONDS.toMillis(10))
    }

    @Test
    fun `keeps a custom export period`() {
        val config = DiskBufferingConfig.create(exportPeriodMillis = TimeUnit.MINUTES.toMillis(15))

        assertThat(config.exportPeriodMillis).isEqualTo(TimeUnit.MINUTES.toMillis(15))
    }

    @Test
    fun `falls back to the default when the export period is not positive`() {
        val config = DiskBufferingConfig.create(exportPeriodMillis = 0)

        assertThat(config.exportPeriodMillis).isEqualTo(TimeUnit.SECONDS.toMillis(10))
    }
}
