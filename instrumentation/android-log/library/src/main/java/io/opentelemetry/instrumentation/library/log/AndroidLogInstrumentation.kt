/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.log

import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.instrumentation.library.log.internal.LogRecordBuilderCreator

@AutoService(AndroidInstrumentation::class)
class AndroidLogInstrumentation : AndroidInstrumentation {
    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        LogRecordBuilderCreator.configure(openTelemetryRum.openTelemetry)
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        LogRecordBuilderCreator.reset()
    }

    override val name: String = "android-log"
}
