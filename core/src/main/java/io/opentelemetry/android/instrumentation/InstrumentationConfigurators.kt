/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import io.opentelemetry.android.common.RumConstants.OTEL_RUM_LOG_TAG
import java.util.ServiceLoader

private typealias Konfigurator = InstrumentationConfigurator<*>

internal class InstrumentationConfigurators
    private constructor(
        private val configurators: Map<Class<out AndroidInstrumentation>, List<Konfigurator>>,
    ) {
        companion object {
            fun create(): InstrumentationConfigurators = create { ServiceLoader.load(Konfigurator::class.java) }

            // Exists for testing
            inline fun create(loader: () -> Iterable<Konfigurator>): InstrumentationConfigurators {
                val configurators = loader().groupBy { it.instrumentationType }
                return InstrumentationConfigurators(configurators)
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun configure(instrumentation: AndroidInstrumentation) {
            configurators[instrumentation.javaClass]
                ?.forEach {
                    try {
                        (it as InstrumentationConfigurator<AndroidInstrumentation>).configure(instrumentation)
                    } catch (e: ClassCastException) {
                        android.util.Log.w(
                            OTEL_RUM_LOG_TAG,
                            "InstrumentationConfigurator '${it.javaClass.name}' could not be applied to instrumentation '${instrumentation.javaClass.name}' (name='${instrumentation.name}').",
                            e,
                        )
                    }
                }
        }
    }
