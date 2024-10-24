/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import android.app.Application
import io.opentelemetry.api.OpenTelemetry

class TestAndroidInstrumentation : AndroidInstrumentation {
    var installed = false
        private set

    override fun install(
        application: Application,
        openTelemetry: OpenTelemetry,
    ) {
        installed = true
    }
}
