/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import org.junit.jupiter.api.Test
import java.io.File

internal class RuntimeDetailsExtractorTest {
    @Test
    fun shouldCollectRuntimeDetails() {
        val file =
            mockk<File>(relaxed = true) {
                every { freeSpace } returns 4200L
            }
        val context =
            mockk<Context>(relaxed = true) {
                every { filesDir } returns file
            }
        val intent =
            mockk<Intent>(relaxed = true) {
                every { getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 690
                every { getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 1000
            }

        val details: RuntimeDetailsExtractor = RuntimeDetailsExtractor.create(context)
        details.onReceive(context, intent)

        val attributes = Attributes.builder()
        val crashDetails = CrashDetails(Thread.currentThread(), NullPointerException())
        attributes.putAll(
            details.extract(
                io.opentelemetry.context.Context
                    .root(),
                crashDetails,
            ),
        )
        OpenTelemetryAssertions
            .assertThat(attributes.build())
            .hasSize(3)
            .containsEntry(RumConstants.STORAGE_SPACE_FREE_KEY, 4200L)
            .containsKey(RumConstants.HEAP_FREE_KEY)
            .containsEntry(RumConstants.BATTERY_PERCENT_KEY, 69.0)
    }
}
