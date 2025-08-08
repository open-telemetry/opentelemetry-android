/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.log

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.instrumentation.library.log.internal.LogRecordBuilderCreator.configure

@AutoService(AndroidInstrumentation::class)
class AndroidLogInstrumentation : AndroidInstrumentation {
    private companion object {
        public const val INSTRUMENTATION_NAME = "android-log"
    }

    override fun install(ctx: InstallationContext) {
        configure(ctx)
    }

    override val name: String = INSTRUMENTATION_NAME
}
