/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.config

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.function.Supplier

class OtelRumConfigTest {
    @Test
    fun `no global attributes by default`() {
        val config = OtelRumConfig()
        assertThat(config.hasGlobalAttributes()).isFalse()
        assertThat(config.getGlobalAttributesSupplier().get().isEmpty).isTrue()
    }

    @Test
    fun `setting null Attributes does nothing`() {
        val config = OtelRumConfig()
        config.setGlobalAttributes(null)
        assertThat(config.hasGlobalAttributes()).isFalse()
        assertThat(config.getGlobalAttributesSupplier().get().isEmpty).isTrue()
    }

    @Test
    fun `setting empty Attributes does nothing`() {
        val config = OtelRumConfig()
        config.setGlobalAttributes(Attributes.empty())
        assertThat(config.hasGlobalAttributes()).isFalse()
        assertThat(config.getGlobalAttributesSupplier().get().isEmpty).isTrue()
    }

    @Test
    fun `can set some Attributes directly`() {
        val config = OtelRumConfig()
        config.setGlobalAttributes(Attributes.of(stringKey("foo"), "bar"))
        assertThat(config.hasGlobalAttributes()).isTrue()
        assertThat(
            config.getGlobalAttributesSupplier().get().get(stringKey("foo")),
        ).isEqualTo("bar")
    }

    @Test
    fun `setting a null attribute supplier does nothing`() {
        val config = OtelRumConfig()
        config.setGlobalAttributes(null as Supplier<Attributes>?)
        assertThat(config.hasGlobalAttributes()).isFalse()
        assertThat(config.getGlobalAttributesSupplier().get().isEmpty).isTrue()
    }

    @Test
    fun `setting a Supplier that returns empty attributes is fine`() {
        val config = OtelRumConfig()
        config.setGlobalAttributes { Attributes.empty() }
        assertThat(config.hasGlobalAttributes()).isTrue() // It might return some Attributes later
        assertThat(config.getGlobalAttributesSupplier().get().isEmpty).isTrue()
    }

    @Test
    fun `can supply global attributes with a supplier`() {
        val config = OtelRumConfig()
        config.setGlobalAttributes { Attributes.of(stringKey("foo"), "bar") }
        assertThat(config.hasGlobalAttributes()).isTrue()
        assertThat(
            config.getGlobalAttributesSupplier().get().get(stringKey("foo")),
        ).isEqualTo("bar")
    }

    @Test
    fun `legacy crash instrumentation name remains supported`() {
        val config = OtelRumConfig()

        config.suppressInstrumentation("crash")
        assertThat(config.isSuppressed("java_crash")).isTrue()
        assertThat(config.isSuppressed("native-crash")).isFalse()

        config.allowInstrumentation("java_crash")
        assertThat(config.isSuppressed("crash")).isFalse()
    }

    @Test
    fun `java crash instrumentation can be allowed using legacy name`() {
        val config = OtelRumConfig()

        config.suppressInstrumentation("java_crash")
        assertThat(config.isSuppressed("crash")).isTrue()
        assertThat(config.isSuppressed("native-crash")).isFalse()

        config.allowInstrumentation("crash")
        assertThat(config.isSuppressed("java_crash")).isFalse()
    }

    @Test
    fun `signals are enabled by default`() {
        val config = OtelRumConfig()
        assertThat(config.tracingEnabled).isTrue()
        assertThat(config.loggingEnabled).isTrue()
        assertThat(config.metricsEnabled).isTrue()
    }

    @Test
    fun `signals can be disabled`() {
        val config = OtelRumConfig()
        assertThat(config.setTracingDisabled().tracingEnabled).isFalse()
        assertThat(config.setLoggingDisabled().loggingEnabled).isFalse()
        assertThat(config.setMetricsDisabled().metricsEnabled).isFalse()
    }
}
