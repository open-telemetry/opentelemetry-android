/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

import android.util.Log
import io.opentelemetry.android.common.RumConstants.OTEL_RUM_LOG_TAG

/**
 * The [ApiExtension] instances available to a single
 * [io.opentelemetry.android.OpenTelemetryRum] instance, keyed by [ApiExtension.type].
 */
internal class ApiExtensionRegistry private constructor(
    private val extensions: Map<Class<out ApiExtension>, ApiExtension>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : ApiExtension> get(type: Class<T>): T? = extensions[type] as? T

    companion object {
        /**
         * Builds the registry from classpath-discovered extensions and explicitly registered ones.
         * Explicitly registered extensions are applied last, so they replace a discovered extension
         * of the same type.
         */
        fun create(
            discovered: List<ApiExtension>,
            registered: List<ApiExtension>,
        ): ApiExtensionRegistry {
            val result = mutableMapOf<Class<out ApiExtension>, ApiExtension>()
            for (extension in discovered + registered) {
                val previous = result.put(extension.type, extension)
                if (previous != null) {
                    Log.w(
                        OTEL_RUM_LOG_TAG,
                        "ApiExtension '${extension.type.name}' provided by '${previous.javaClass.name}' " +
                            "was replaced by '${extension.javaClass.name}'.",
                    )
                }
            }
            return ApiExtensionRegistry(result)
        }
    }
}
