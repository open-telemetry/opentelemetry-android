/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.test.common

import io.opentelemetry.api.common.AttributeKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RobolectricOpenTelemetryRumRuleTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `captures full fidelity observations when enabled`() {
        val outputDirectory = temporaryFolder.newFolder()
        val rumRule = RobolectricOpenTelemetryRumRule(true, outputDirectory)
        val statement =
            rumRule.apply(
                object : Statement() {
                    override fun evaluate() {
                        val openTelemetry = rumRule.openTelemetryRum.openTelemetry
                        openTelemetry
                            .getTracer("toy.scope")
                            .spanBuilder("toy.span")
                            .setAttribute("toy.span.attribute", "secret-span-value")
                            .setAttribute(
                                AttributeKey.stringArrayKey("toy.span.array"),
                                listOf("first", "second"),
                            ).startSpan()
                            .end()
                        openTelemetry
                            .logsBridge
                            .loggerBuilder("toy.scope")
                            .build()
                            .logRecordBuilder()
                            .setEventName("toy.event")
                            .setAttribute(AttributeKey.longKey("toy.event.attribute"), 42)
                            .emit()
                    }
                },
                Description.createTestDescription(javaClass, "toy telemetry"),
            )

        statement.evaluate()

        val observation =
            outputDirectory
                .listFiles()
                .orEmpty()
                .single()
                .readText()
        assertThat(observation)
            .contains(
                "\"type\":\"span\",\"name\":\"toy.span\",\"scope\":\"toy.scope\"",
                "\"type\":\"event\",\"name\":\"toy.event\",\"scope\":\"toy.scope\"",
                "\"name\":\"toy.span.attribute\",\"type\":\"string\"",
                "\"name\":\"toy.span.array\",\"type\":\"string_array\"",
                "\"name\":\"toy.event.attribute\",\"type\":\"int\"",
                "\"value\":\"secret-span-value\"",
                "\"value\":[\"first\",\"second\"]",
                "\"value\":42",
            )
    }

    @Test
    fun `does not write observations when disabled`() {
        val outputDirectory = temporaryFolder.newFolder()
        val rumRule = RobolectricOpenTelemetryRumRule(false, outputDirectory)
        val statement =
            rumRule.apply(
                object : Statement() {
                    override fun evaluate() {
                        rumRule.getSpan().end()
                    }
                },
                Description.createTestDescription(javaClass, "capture disabled"),
            )

        statement.evaluate()

        assertThat(outputDirectory).isEmptyDirectory()
    }
}
