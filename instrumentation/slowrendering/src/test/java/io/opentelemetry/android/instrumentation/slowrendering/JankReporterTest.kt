/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.util.SparseIntArray
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

class JankReporterTest {
    @Test
    fun combine() {
        val state = StringBuilder("")
        val inner =
            object : JankReporter {
                override fun reportSlow(
                    durationToCountHistogram: SparseIntArray,
                    periodSeconds: Double,
                    activityName: String,
                ) {
                    state
                        .append(".inner.")
                        .append(durationToCountHistogram)
                        .append(".")
                        .append(periodSeconds)
                        .append(".")
                        .append(activityName)
                }
            }
        val outer =
            object : JankReporter {
                override fun reportSlow(
                    durationToCountHistogram: SparseIntArray,
                    periodSeconds: Double,
                    activityName: String,
                ) {
                    state
                        .append(".outer.")
                        .append(durationToCountHistogram)
                        .append(".")
                        .append(periodSeconds)
                        .append(".")
                        .append(activityName)
                }
            }

        val both = inner.combine(outer)

        val histogram = SparseIntArray()
        val histogramData: SparseIntArray = mockk()
        every { histogramData.size() } returns 1
        every { histogramData.toString() } returns "x"
        val key1 = 99
        every { histogramData.keyAt(0) } returns key1
        every { histogramData.get(key1) } returns 37
        both.reportSlow(histogram, 6.9, "four.something")
        val expected = ".inner.null.6.9.four.something.outer.null.6.9.four.something"
        assertThat(state.toString()).isEqualTo(expected)
    }

    @Test
    fun `combine with self fails`() {
        val state = StringBuilder("")
        val reporter =
            object : JankReporter {
                override fun reportSlow(
                    durationToCountHistogram: SparseIntArray,
                    periodSeconds: Double,
                    activityName: String,
                ) {
                }
            }
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { reporter.combine(reporter) }
            .withMessage("cannot combine with self")
    }
}
