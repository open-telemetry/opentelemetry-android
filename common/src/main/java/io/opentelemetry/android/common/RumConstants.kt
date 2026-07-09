/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common

import io.opentelemetry.android.semconv.LastAttributes
import io.opentelemetry.api.common.AttributeKey

object RumConstants {
    const val OTEL_RUM_LOG_TAG: String = "OpenTelemetryRum"

    @Deprecated("Use LastAttributes.LAST_SCREEN_NAME_KEY instead.")
    @JvmField
    val LAST_SCREEN_NAME_KEY: AttributeKey<String> = LastAttributes.LAST_SCREEN_NAME_KEY
}
