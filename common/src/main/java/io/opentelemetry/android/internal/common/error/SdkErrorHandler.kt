/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.common.error

import java.util.concurrent.CancellationException

/**
 * This interface is internal and not for public use. Its APIs are unstable and can change at any time.
 *
 * Handler for [SdkError]s encountered during OpenTelemetry Android SDK execution.
 */
fun interface SdkErrorHandler {
    fun onError(error: SdkError)

    companion object {
        val NOOP: SdkErrorHandler = SdkErrorHandler { }
    }
}

/**
 * Safely executes [block]. If an exception is thrown, it is reported to this [SdkErrorHandler] via [errorFactory],
 * and [default] is returned without crashing the host application.
 *
 * [CancellationException] is rethrown immediately without reporting to avoid disrupting coroutine cancellation.
 */
inline fun <T> SdkErrorHandler.guarded(
    default: T,
    errorFactory: (Throwable) -> SdkError,
    block: () -> T,
): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        try {
            onError(errorFactory(t))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Suppress reporting failures to guarantee fallback is returned
        }
        default
    }

/**
 * Safely executes [block]. If an exception is thrown, it is reported to this [SdkErrorHandler] via [errorFactory],
 * and `null` is returned without crashing the host application.
 *
 * [CancellationException] is rethrown immediately without reporting to avoid disrupting coroutine cancellation.
 */
inline fun <T> SdkErrorHandler.guarded(
    errorFactory: (Throwable) -> SdkError,
    block: () -> T,
): T? =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        try {
            onError(errorFactory(t))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Suppress reporting failures to guarantee fallback is returned
        }
        null
    }

/**
 * Executes [block], reporting any thrown exception as an [SdkError.UserCode].
 */
inline fun <T> SdkErrorHandler.guardedUserCode(
    description: String,
    default: T,
    block: () -> T,
): T = guarded(default, { SdkError.UserCode(it, description) }, block)

/**
 * Executes [block], reporting any thrown exception as an [SdkError.UserCode].
 */
inline fun <T> SdkErrorHandler.guardedUserCode(
    description: String,
    block: () -> T,
): T? = guarded({ SdkError.UserCode(it, description) }, block)

/**
 * Executes [block], reporting any thrown exception as an [SdkError.SdkOperation].
 */
inline fun <T> SdkErrorHandler.guardedSdkOperation(
    description: String,
    default: T,
    block: () -> T,
): T = guarded(default, { SdkError.SdkOperation(it, description) }, block)

/**
 * Executes [block], reporting any thrown exception as an [SdkError.SdkOperation].
 */
inline fun <T> SdkErrorHandler.guardedSdkOperation(
    description: String,
    block: () -> T,
): T? = guarded({ SdkError.SdkOperation(it, description) }, block)

/**
 * Executes [block], reporting any thrown exception as an [SdkError.ApiMisuse].
 */
inline fun <T> SdkErrorHandler.guardedApiMisuse(
    description: String,
    default: T,
    block: () -> T,
): T = guarded(default, { SdkError.ApiMisuse(it, description) }, block)

/**
 * Executes [block], reporting any thrown exception as an [SdkError.ApiMisuse].
 */
inline fun <T> SdkErrorHandler.guardedApiMisuse(
    description: String,
    block: () -> T,
): T? = guarded({ SdkError.ApiMisuse(it, description) }, block)
