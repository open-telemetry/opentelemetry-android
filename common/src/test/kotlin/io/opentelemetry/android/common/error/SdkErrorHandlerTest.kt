/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.error

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

internal class SdkErrorHandlerTest {
    @Test
    fun `guarded with default returns block result when no exception is thrown`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }

        val result =
            handler.guarded(default = 0, errorFactory = { SdkError.SdkBug(it, "desc") }) {
                42
            }

        assertThat(result).isEqualTo(42)
        assertThat(captured).isEmpty()
    }

    @Test
    fun `guarded with default catches exception reports error and returns default`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val exception = RuntimeException("failure")

        val result =
            handler.guarded(default = -1, errorFactory = { SdkError.SdkBug(it, "failed calculation") }) {
                throw exception
            }

        assertThat(result).isEqualTo(-1)
        assertThat(captured).hasSize(1)
        val error = captured.first() as SdkError.SdkBug
        assertThat(error.throwable).isSameAs(exception)
        assertThat(error.description).isEqualTo("failed calculation")
    }

    @Test
    fun `guarded without default returns null on failure`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val exception = IllegalStateException("boom")

        val result: String? =
            handler.guarded(errorFactory = { SdkError.UserCode(it, "action") }) {
                throw exception
            }

        assertThat(result).isNull()
        assertThat(captured).hasSize(1)
        val error = captured.first() as SdkError.UserCode
        assertThat(error.throwable).isSameAs(exception)
        assertThat(error.description).isEqualTo("action")
    }

    @Test
    fun `guardedUserCode with default catches and reports SdkError UserCode`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val exception = RuntimeException("user callback error")

        val result =
            handler.guardedUserCode(description = "listener execution", default = "fallback") {
                throw exception
            }

        assertThat(result).isEqualTo("fallback")
        assertThat(captured).hasSize(1)
        val error = captured.first()
        assertThat(error).isInstanceOf(SdkError.UserCode::class.java)
        assertThat(error.throwable).isSameAs(exception)
        assertThat(error.description).isEqualTo("listener execution")
    }

    @Test
    fun `guardedUserCode without default catches and returns null`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val exception = RuntimeException("user error")

        val result: Int? =
            handler.guardedUserCode(description = "extractor") {
                throw exception
            }

        assertThat(result).isNull()
        assertThat(captured).hasSize(1)
        assertThat(captured.first()).isInstanceOf(SdkError.UserCode::class.java)
    }

    @Test
    fun `guardedSdkBug with default catches and reports SdkError SdkBug`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val exception = NullPointerException("npe")

        val result =
            handler.guardedSdkBug(description = "internal task", default = false) {
                throw exception
            }

        assertThat(result).isFalse()
        assertThat(captured).hasSize(1)
        val error = captured.first()
        assertThat(error).isInstanceOf(SdkError.SdkBug::class.java)
        assertThat(error.throwable).isSameAs(exception)
        assertThat(error.description).isEqualTo("internal task")
    }

    @Test
    fun `guardedSdkBug without default catches and returns null`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val exception = RuntimeException("bug")

        val result: String? =
            handler.guardedSdkBug(description = "buggy code") {
                throw exception
            }

        assertThat(result).isNull()
        assertThat(captured).hasSize(1)
        assertThat(captured.first()).isInstanceOf(SdkError.SdkBug::class.java)
    }

    @Test
    fun `guardedApiMisuse with default catches and reports SdkError ApiMisuse`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val exception = IllegalArgumentException("invalid")

        val result =
            handler.guardedApiMisuse(description = "builder validation", default = "default") {
                throw exception
            }

        assertThat(result).isEqualTo("default")
        assertThat(captured).hasSize(1)
        val error = captured.first()
        assertThat(error).isInstanceOf(SdkError.ApiMisuse::class.java)
        assertThat(error.throwable).isSameAs(exception)
        assertThat(error.description).isEqualTo("builder validation")
    }

    @Test
    fun `guardedApiMisuse without default catches and returns null`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val exception = IllegalArgumentException("misuse")

        val result: Long? =
            handler.guardedApiMisuse(description = "misuse check") {
                throw exception
            }

        assertThat(result).isNull()
        assertThat(captured).hasSize(1)
        assertThat(captured.first()).isInstanceOf(SdkError.ApiMisuse::class.java)
    }

    @Test
    fun `NOOP handler safely ignores errors without throwing`() {
        assertThatCode {
            SdkErrorHandler.NOOP.onError(SdkError.SdkBug(RuntimeException(), "noop test"))
        }.doesNotThrowAnyException()
    }
}
