/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.common.error

import android.util.Log
import io.opentelemetry.android.common.RumConstants

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 *
 * Default implementation of [SdkErrorHandler] that logs errors via Android [Log] under [logTag].
 */
class DefaultSdkErrorHandler(
    private val logTag: String = RumConstants.OTEL_RUM_LOG_TAG,
) : SdkErrorHandler {
    override fun onError(error: SdkError) {
        when (error) {
            is SdkError.SdkBug -> {
                Log.w(logTag, "Internal SDK error: ${error.description}", error.throwable)
            }

            is SdkError.UserCode -> {
                Log.w(logTag, "Error in user-provided code: ${error.description}", error.throwable)
            }

            is SdkError.ApiMisuse -> {
                Log.w(logTag, "API misuse error: ${error.description}", error.throwable)
            }
        }
    }

    companion object {
        val INSTANCE: DefaultSdkErrorHandler = DefaultSdkErrorHandler()
    }
}
