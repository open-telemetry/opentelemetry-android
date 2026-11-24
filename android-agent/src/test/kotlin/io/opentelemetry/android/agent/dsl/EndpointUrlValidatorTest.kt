/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class EndpointUrlValidatorTest {
    @Test
    fun `rejects invalid explicit ports`() {
        listOf(
            "http://127.0.0.1:65536/v1/traces",
            "http://collector_host:65536/v1/traces",
            "http://[::1]:65536/v1/traces",
            "http://localhost:0/v1/traces",
            "http://localhost:-1/v1/traces",
            "http://localhost:/v1/traces",
            "http://localhost:invalid/v1/traces",
            "http://localhost:999999999999/v1/traces",
        ).forEach { endpoint ->
            assertThatThrownBy { EndpointUrlValidator.validate(endpoint) }
                .describedAs(endpoint)
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `accepts absent ports and valid explicit ports`() {
        listOf(
            "http://localhost/v1/traces",
            "https://localhost:1/v1/traces",
            "http://127.0.0.1:65535/v1/traces",
            "http://collector_host:4318/v1/traces",
            "http://[::1]/v1/traces",
            "http://[::1]:4318/v1/traces",
            "http://user:password@localhost/v1/traces",
        ).forEach { endpoint ->
            assertThatCode { EndpointUrlValidator.validate(endpoint) }
                .describedAs(endpoint)
                .doesNotThrowAnyException()
        }
    }
}
