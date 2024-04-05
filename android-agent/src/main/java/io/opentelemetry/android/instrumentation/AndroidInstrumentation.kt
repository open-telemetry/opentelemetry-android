/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import android.app.Application
import io.opentelemetry.android.OpenTelemetryRum

interface AndroidInstrumentation {
    fun apply(
        application: Application,
        openTelemetryRum: OpenTelemetryRum,
    )
}
