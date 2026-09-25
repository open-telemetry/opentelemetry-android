/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.dsl.instrumentation.HttpTelemetryConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.function.Predicate

class HttpTelemetryConfigurationTest {
    @Test
    fun noPredicateIsSetUntilOneIsConfigured() {
        assertThat(HttpTelemetryConfiguration().recordSpanForHost()).isNull()
    }

    @Test
    fun theConfiguredPredicateIsHandedOnAsIs() {
        val predicate = Predicate<String> { it == "api.example.com" }
        val config = HttpTelemetryConfiguration().apply { shouldRecordSpanForHost(predicate) }

        assertThat(config.recordSpanForHost()).isSameAs(predicate)
    }

    @Test
    fun theLastPredicateWins() {
        val config =
            HttpTelemetryConfiguration().apply {
                shouldRecordSpanForHost { it == "first.example.com" }
                shouldRecordSpanForHost { it == "second.example.com" }
            }

        val predicate = checkNotNull(config.recordSpanForHost())
        assertThat(predicate.test("first.example.com")).isFalse()
        assertThat(predicate.test("second.example.com")).isTrue()
    }
}
