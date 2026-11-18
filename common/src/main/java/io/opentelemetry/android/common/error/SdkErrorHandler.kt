/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.error

/**
 * Handler for [SdkError]s encountered during OpenTelemetry Android SDK execution.
 */
internal fun interface SdkErrorHandler {
    fun onError(error: SdkError)

    companion object {
        val NOOP: SdkErrorHandler = SdkErrorHandler { }
    }
}

/**
 * Safely executes [block]. If an exception is thrown, it is reported to this [SdkErrorHandler] via [errorFactory],
 * and [default] is returned without crashing the host application.
 */
internal inline fun <T> SdkErrorHandler.guarded(
    default: T,
    errorFactory: (Throwable) -> SdkError,
    block: () -> T,
): T =
    try {
        block()
    } catch (t: Throwable) {
        onError(errorFactory(t))
        default
    }

/**
 * Safely executes [block]. If an exception is thrown, it is reported to this [SdkErrorHandler] via [errorFactory],
 * and `null` is returned without crashing the host application.
 */
internal inline fun <T> SdkErrorHandler.guarded(
    errorFactory: (Throwable) -> SdkError,
    block: () -> T,
): T? =
    try {
        block()
    } catch (t: Throwable) {
        onError(errorFactory(t))
        null
    }

/**
 * Executes [block], reporting any thrown exception as an [SdkError.UserCode].
 */
internal inline fun <T> SdkErrorHandler.guardedUserCode(
    description: String,
    default: T,
    block: () -> T,
): T = guarded(default, { SdkError.UserCode(it, description) }, block)

/**
 * Executes [block], reporting any thrown exception as an [SdkError.UserCode].
 */
internal inline fun <T> SdkErrorHandler.guardedUserCode(
    description: String,
    block: () -> T,
): T? = guarded({ SdkError.UserCode(it, description) }, block)

/**
 * Executes [block], reporting any thrown exception as an [SdkError.SdkBug].
 */
internal inline fun <T> SdkErrorHandler.guardedSdkBug(
    description: String,
    default: T,
    block: () -> T,
): T = guarded(default, { SdkError.SdkBug(it, description) }, block)

/**
 * Executes [block], reporting any thrown exception as an [SdkError.SdkBug].
 */
internal inline fun <T> SdkErrorHandler.guardedSdkBug(
    description: String,
    block: () -> T,
): T? = guarded({ SdkError.SdkBug(it, description) }, block)

/**
 * Executes [block], reporting any thrown exception as an [SdkError.ApiMisuse].
 */
internal inline fun <T> SdkErrorHandler.guardedApiMisuse(
    description: String,
    default: T,
    block: () -> T,
): T = guarded(default, { SdkError.ApiMisuse(it, description) }, block)

/**
 * Executes [block], reporting any thrown exception as an [SdkError.ApiMisuse].
 */
internal inline fun <T> SdkErrorHandler.guardedApiMisuse(
    description: String,
    block: () -> T,
): T? = guarded({ SdkError.ApiMisuse(it, description) }, block)
