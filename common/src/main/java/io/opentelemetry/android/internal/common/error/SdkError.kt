/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.common.error

/**
 * This interface is internal and not for public use. Its APIs are unstable and can change at any time.
 *
 * Represents an error encountered by the OpenTelemetry Android SDK.
 */
sealed interface SdkError {
    val throwable: Throwable
    val description: String

    /**
     * An unexpected internal error or operation failure within the OpenTelemetry Android SDK.
     */
    data class SdkOperation(
        override val throwable: Throwable,
        override val description: String,
    ) : SdkError

    /**
     * An error thrown from user-provided code, such as custom attribute extractors, callbacks, or listeners.
     */
    data class UserCode(
        override val throwable: Throwable,
        override val description: String,
    ) : SdkError

    /**
     * An error caused by invalid SDK configuration or API misuse.
     */
    data class ApiMisuse(
        override val throwable: Throwable,
        override val description: String,
    ) : SdkError
}
