/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.common.error

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException

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
        val exception = IllegalStateException("failure")

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
    fun `guarded with default rethrows CancellationException without reporting`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val cancellation = CancellationException("coroutine cancelled")

        assertThatThrownBy {
            handler.guarded(default = "fallback", errorFactory = { SdkError.SdkBug(it, "desc") }) {
                throw cancellation
            }
        }.isSameAs(cancellation)

        assertThat(captured).isEmpty()
    }

    @Test
    fun `guarded without default rethrows CancellationException without reporting`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val cancellation = CancellationException("coroutine cancelled")

        assertThatThrownBy {
            handler.guarded(errorFactory = { SdkError.SdkBug(it, "desc") }) {
                throw cancellation
            }
        }.isSameAs(cancellation)

        assertThat(captured).isEmpty()
    }

    @Test
    fun `guarded with default returns fallback even if handler throws`() {
        val throwingHandler =
            SdkErrorHandler {
                throw IllegalStateException("handler crashed")
            }

        val resultWithDefault =
            throwingHandler.guarded(default = "fallback", errorFactory = { SdkError.SdkBug(it, "desc") }) {
                throw IllegalStateException("block failure")
            }
        assertThat(resultWithDefault).isEqualTo("fallback")
    }

    @Test
    fun `guarded without default returns null even if handler throws`() {
        val throwingHandler =
            SdkErrorHandler {
                throw IllegalStateException("handler crashed")
            }

        val resultWithoutDefault: String? =
            throwingHandler.guarded(errorFactory = { SdkError.SdkBug(it, "desc") }) {
                throw IllegalStateException("block failure")
            }
        assertThat(resultWithoutDefault).isNull()
    }

    @Test
    fun `guarded with default returns fallback even if errorFactory throws`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }

        val resultWithDefault =
            handler.guarded(
                default = 100,
                errorFactory = { throw IllegalStateException("factory crashed") },
            ) {
                throw IllegalStateException("block failure")
            }
        assertThat(resultWithDefault).isEqualTo(100)
        assertThat(captured).isEmpty()
    }

    @Test
    fun `guarded without default returns null even if errorFactory throws`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }

        val resultWithoutDefault: Int? =
            handler.guarded(
                errorFactory = { throw IllegalStateException("factory crashed") },
            ) {
                throw IllegalStateException("block failure")
            }
        assertThat(resultWithoutDefault).isNull()
        assertThat(captured).isEmpty()
    }

    @Test
    fun `guarded with default rethrows CancellationException thrown by handler`() {
        val cancellation = CancellationException("handler cancelled")
        val handler =
            SdkErrorHandler {
                throw cancellation
            }

        assertThatThrownBy {
            handler.guarded(default = "fallback", errorFactory = { SdkError.SdkBug(it, "desc") }) {
                throw IllegalStateException("block failure")
            }
        }.isSameAs(cancellation)
    }

    @Test
    fun `guarded without default rethrows CancellationException thrown by handler`() {
        val cancellation = CancellationException("handler cancelled")
        val handler =
            SdkErrorHandler {
                throw cancellation
            }

        assertThatThrownBy {
            handler.guarded(errorFactory = { SdkError.SdkBug(it, "desc") }) {
                throw IllegalStateException("block failure")
            }
        }.isSameAs(cancellation)
    }

    @Test
    fun `guarded with default rethrows CancellationException thrown by errorFactory`() {
        val cancellation = CancellationException("factory cancelled")
        val handler = SdkErrorHandler { }

        assertThatThrownBy {
            handler.guarded(
                default = "fallback",
                errorFactory = { throw cancellation },
            ) {
                throw IllegalStateException("block failure")
            }
        }.isSameAs(cancellation)
    }

    @Test
    fun `guarded without default rethrows CancellationException thrown by errorFactory`() {
        val cancellation = CancellationException("factory cancelled")
        val handler = SdkErrorHandler { }

        assertThatThrownBy {
            handler.guarded(
                errorFactory = { throw cancellation },
            ) {
                throw IllegalStateException("block failure")
            }
        }.isSameAs(cancellation)
    }

    @Test
    fun `guardedUserCode with default catches and reports SdkError UserCode`() {
        val captured = mutableListOf<SdkError>()
        val handler = SdkErrorHandler { captured.add(it) }
        val exception = IllegalStateException("user callback error")

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
        val exception = IllegalStateException("user error")

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
        val exception = IllegalStateException("bug")

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
            SdkErrorHandler.NOOP.onError(SdkError.SdkBug(IllegalStateException(), "noop test"))
        }.doesNotThrowAnyException()
    }
}
