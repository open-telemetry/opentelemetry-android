/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common

import io.opentelemetry.api.common.AttributeKey

object RumConstants {
    const val OTEL_RUM_LOG_TAG: String = "OpenTelemetryRum"

    @JvmField
    val LAST_SCREEN_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("last.screen.name")

    @JvmField
    val SCREEN_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("screen.name")
}
