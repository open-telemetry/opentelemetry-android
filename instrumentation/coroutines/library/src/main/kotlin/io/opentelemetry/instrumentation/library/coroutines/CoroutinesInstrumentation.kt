/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.coroutines

import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.instrumentation.library.coroutines.internal.CoroutinesContextHelper

@AutoService(AndroidInstrumentation::class)
class CoroutinesInstrumentation : AndroidInstrumentation {
    override val name: String = "coroutines"

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        CoroutinesContextHelper.setEnabled(true)
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        CoroutinesContextHelper.setEnabled(false)
    }
}
