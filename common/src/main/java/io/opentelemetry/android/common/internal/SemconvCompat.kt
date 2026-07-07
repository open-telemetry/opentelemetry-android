/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal

import io.opentelemetry.kotlin.semconv.AppAttributes
import io.opentelemetry.kotlin.semconv.IncubatingApi

/**
 * Internal class used to map the latest/experimental semantic conventions
 * to a previous value. This class is subject to change at any time, and external
 * users are highly discouraged from using it.
 */
@OptIn(IncubatingApi::class)
class SemconvCompat internal constructor() {
    companion object {
        var useLatestExperimental = true

        fun map(key: String): String {
            if (useLatestExperimental) {
                return key
            }
            return when (key) {
                // new -> old
                "app.crash" -> "device.crash"

                AppAttributes.APP_SCREEN_NAME -> "screen.name"

                else -> key
            }
        }
    }
}
