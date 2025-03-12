/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

class TestAndroidInstrumentation : AndroidInstrumentation {
    override val name: String = "test"

    var installed = false
        private set

    override fun install(ctx: InstallationContext) {
        installed = true
    }
}
