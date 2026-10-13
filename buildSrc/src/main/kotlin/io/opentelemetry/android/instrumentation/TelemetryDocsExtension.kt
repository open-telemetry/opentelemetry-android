/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

abstract class TelemetryDocsExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /**
         * Instrumentation scope names whose telemetry belongs to this module.
         *
         * Example: `scopeNames.add("io.opentelemetry.android.instrumentation.view.click")`.
         */
        val scopeNames: ListProperty<String> = objects.listProperty(String::class.java)

        init {
            scopeNames.convention(emptyList())
        }

        fun scopeNames(vararg names: String) {
            scopeNames.addAll(*names)
        }
    }
