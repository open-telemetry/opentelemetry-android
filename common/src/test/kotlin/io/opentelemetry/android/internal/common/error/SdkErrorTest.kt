/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.common.error

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SdkErrorTest {
    @Test
    fun `SdkOperation stores throwable and description`() {
        val throwable = IllegalStateException("boom")
        val error = SdkError.SdkOperation(throwable, "unexpected crash")

        assertThat(error.throwable).isSameAs(throwable)
        assertThat(error.description).isEqualTo("unexpected crash")
    }

    @Test
    fun `UserCode stores throwable and description`() {
        val throwable = IllegalStateException("user error")
        val error = SdkError.UserCode(throwable, "custom callback failed")

        assertThat(error.throwable).isSameAs(throwable)
        assertThat(error.description).isEqualTo("custom callback failed")
    }

    @Test
    fun `ApiMisuse stores throwable and description`() {
        val throwable = IllegalArgumentException("bad argument")
        val error = SdkError.ApiMisuse(throwable, "invalid configuration")

        assertThat(error.throwable).isSameAs(throwable)
        assertThat(error.description).isEqualTo("invalid configuration")
    }

    @Test
    fun `exhaustive pattern matching works across all SdkError subtypes`() {
        val errors: List<SdkError> =
            listOf(
                SdkError.SdkOperation(IllegalStateException(), "operation"),
                SdkError.UserCode(IllegalStateException(), "user"),
                SdkError.ApiMisuse(IllegalArgumentException(), "misuse"),
            )

        val categories =
            errors.map { error ->
                when (error) {
                    is SdkError.SdkOperation -> "sdk_operation"
                    is SdkError.UserCode -> "user_code"
                    is SdkError.ApiMisuse -> "api_misuse"
                }
            }

        assertThat(categories).containsExactly("sdk_operation", "user_code", "api_misuse")
    }
}
