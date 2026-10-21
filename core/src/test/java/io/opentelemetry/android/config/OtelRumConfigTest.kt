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
    fun `java crash instrumentation can be suppressed and allowed by its name`() {
        val config = OtelRumConfig()

        config.suppressInstrumentation("java-crash")
        assertThat(config.isSuppressed("java-crash")).isTrue()
        assertThat(config.isSuppressed("native-crash")).isFalse()

        config.allowInstrumentation("java-crash")
        assertThat(config.isSuppressed("java-crash")).isFalse()
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

    @Test
    fun `sdk initialization events are disabled by default`() {
        val config = OtelRumConfig()
        assertThat(config.shouldGenerateSdkInitializationEvents()).isFalse()
    }

    @Test
    fun `can enable sdk initialization events`() {
        val config = OtelRumConfig()
        config.enableSdkInitializationEvents()
        assertThat(config.shouldGenerateSdkInitializationEvents()).isTrue()
    }

    @Test
    fun `can disable sdk initialization events`() {
        val config = OtelRumConfig()
        config.enableSdkInitializationEvents()
        config.disableSdkInitializationEvents()
        assertThat(config.shouldGenerateSdkInitializationEvents()).isFalse()
    }
}
