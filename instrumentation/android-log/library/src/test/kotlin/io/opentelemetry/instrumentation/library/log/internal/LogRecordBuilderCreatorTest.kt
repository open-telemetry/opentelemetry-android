/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.log.internal

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.instrumentation.library.log.AndroidLogInstrumentation
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class LogRecordBuilderCreatorTest {
    companion object {
        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    @BeforeEach
    fun setUp() {
        LogRecordBuilderCreator.reset()
    }

    @AfterEach
    fun tearDown() {
        LogRecordBuilderCreator.reset()
    }

    @Test
    fun testConfigureAndReset() {
        LogRecordBuilderCreator.configure(otelTesting.openTelemetry)
        LogRecordBuilderCreator.createLogRecordBuilder().setBody("test message").emit()
        assertThat(otelTesting.logRecords).hasSize(1)

        otelTesting.clearLogRecords()
        LogRecordBuilderCreator.reset()
        LogRecordBuilderCreator.createLogRecordBuilder().setBody("after reset").emit()
        assertThat(otelTesting.logRecords).isEmpty()
    }

    @Test
    fun testInstrumentationInstallAndUninstall() {
        val instrumentation = AndroidLogInstrumentation()
        val context = mockk<Context>()
        val rum =
            mockk<OpenTelemetryRum> {
                every { openTelemetry } returns otelTesting.openTelemetry
            }

        instrumentation.install(context, rum)
        LogRecordBuilderCreator.createLogRecordBuilder().setBody("installed").emit()
        assertThat(otelTesting.logRecords).hasSize(1)

        otelTesting.clearLogRecords()
        instrumentation.uninstall(context, rum)
        LogRecordBuilderCreator.createLogRecordBuilder().setBody("uninstalled").emit()
        assertThat(otelTesting.logRecords).isEmpty()
    }

    @Test
    fun testUninstallBeforeInstallIsSafeNoop() {
        val instrumentation = AndroidLogInstrumentation()
        val context = mockk<Context>()
        val rum = mockk<OpenTelemetryRum>()

        instrumentation.uninstall(context, rum)
        LogRecordBuilderCreator.createLogRecordBuilder().setBody("noop").emit()
        assertThat(otelTesting.logRecords).isEmpty()
    }
}
