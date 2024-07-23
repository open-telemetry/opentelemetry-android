/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.startup

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation

class StartupInstrumentation : AndroidInstrumentation {
    override fun install(
        application: Application,
        openTelemetryRum: OpenTelemetryRum,
    ) {
    }
}
