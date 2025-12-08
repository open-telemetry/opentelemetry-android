/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

class JankReporterTest {
    @Test
    fun combine() {
        val state = StringBuilder("")
        val inner =
            JankReporter { durationToCountHistogram, periodSeconds, activityName ->
                state
                    .append(".inner.")
                    .append(durationToCountHistogram)
                    .append(".")
                    .append(periodSeconds)
                    .append(".")
                    .append(activityName)
            }
        val outer =
            JankReporter { durationToCountHistogram, periodSeconds, activityName ->
                state
                    .append(".outer.")
                    .append(durationToCountHistogram)
                    .append(".")
                    .append(periodSeconds)
                    .append(".")
                    .append(activityName)
            }

        val both = inner.combine(outer)

        val histogram = HashMap<Int, Int>()
        histogram[99] = 37
        both.reportSlow(histogram, 6.9, "four.something")
        val expected = ".inner.{99=37}.6.9.four.something.outer.{99=37}.6.9.four.something"
        assertThat(state.toString()).isEqualTo(expected)
    }

    @Test
    fun `combine with self fails`() {
        val reporter =
            JankReporter { _, _, _ -> }
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { reporter.combine(reporter) }
            .withMessage("cannot combine with self")
    }
}
