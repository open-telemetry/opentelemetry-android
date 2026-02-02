/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ExportProtocolTest {
    @Test
    fun `HTTP protocol has correct name`() {
        assertThat(ExportProtocol.HTTP.name).isEqualTo("HTTP")
    }

    @Test
    fun `GRPC protocol has correct name`() {
        assertThat(ExportProtocol.GRPC.name).isEqualTo("GRPC")
    }

    @Test
    fun `values returns both protocols`() {
        val values = ExportProtocol.values()

        assertThat(values).hasSize(2)
        assertThat(values).contains(ExportProtocol.HTTP)
        assertThat(values).contains(ExportProtocol.GRPC)
    }

    @Test
    fun `valueOf HTTP returns HTTP`() {
        assertThat(ExportProtocol.valueOf("HTTP")).isEqualTo(ExportProtocol.HTTP)
    }

    @Test
    fun `valueOf GRPC returns GRPC`() {
        assertThat(ExportProtocol.valueOf("GRPC")).isEqualTo(ExportProtocol.GRPC)
    }

    @Test
    fun `HTTP ordinal is 0`() {
        assertThat(ExportProtocol.HTTP.ordinal).isEqualTo(0)
    }

    @Test
    fun `GRPC ordinal is 1`() {
        assertThat(ExportProtocol.GRPC.ordinal).isEqualTo(1)
    }
}
