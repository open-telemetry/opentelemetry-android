/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import android.content.Context
import io.opentelemetry.android.OpenTelemetryRum

class TestAndroidInstrumentation : AndroidInstrumentation {
    override val name: String = "test"

    var installed = false
        private set

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        installed = true
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        installed = false
    }
}
