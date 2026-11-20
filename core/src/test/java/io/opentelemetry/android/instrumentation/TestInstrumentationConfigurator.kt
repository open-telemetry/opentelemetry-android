/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

class TestInstrumentationConfigurator : InstrumentationConfigurator<TestAndroidInstrumentation> {
    init {
        check(!failOnCreate) { "TestInstrumentationConfigurator failed to load" }
    }

    override val instrumentationType = TestAndroidInstrumentation::class.java

    override fun configure(instrumentation: TestAndroidInstrumentation) {
    }

    companion object {
        var failOnCreate = false
    }
}
