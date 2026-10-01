/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.dsl.instrumentation.HttpTelemetryConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(Incubating::class)
class HttpTelemetryConfigurationTest {
    @Test
    fun noPredicateIsSetUntilOneIsConfigured() {
        assertThat(HttpTelemetryConfiguration().recordSpanForHost()).isNull()
    }

    @Test
    fun theConfiguredPredicateIsWhatGetsHandedOn() {
        val config = HttpTelemetryConfiguration().apply { shouldRecordSpanForHost { it == "api.example.com" } }

        val predicate = checkNotNull(config.recordSpanForHost())
        assertThat(predicate("api.example.com")).isTrue()
        assertThat(predicate("other.example.com")).isFalse()
    }

    @Test
    fun theLastPredicateWins() {
        val config =
            HttpTelemetryConfiguration().apply {
                shouldRecordSpanForHost { it == "first.example.com" }
                shouldRecordSpanForHost { it == "second.example.com" }
            }

        val predicate = checkNotNull(config.recordSpanForHost())
        assertThat(predicate("first.example.com")).isFalse()
        assertThat(predicate("second.example.com")).isTrue()
    }
}
