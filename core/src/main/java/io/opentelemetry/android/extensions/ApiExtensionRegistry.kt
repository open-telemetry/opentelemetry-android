/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

import android.util.Log
import io.opentelemetry.android.common.RumConstants.OTEL_RUM_LOG_TAG

internal class ApiExtensionRegistry private constructor(
    private val extensions: Map<Class<out ApiExtension>, ApiExtension>,
) {
    fun <T : ApiExtension> get(type: Class<T>): T? = type.cast(extensions[type])

    companion object {
        fun create(
            discovered: List<ApiExtension>,
            registered: List<ApiExtension>,
        ): ApiExtensionRegistry {
            val result = mutableMapOf<Class<out ApiExtension>, ApiExtension>()
            for (extension in discovered + registered) {
                val type = extension.type
                val previous = result.put(type, extension)
                if (previous != null) {
                    Log.w(
                        OTEL_RUM_LOG_TAG,
                        "ApiExtension '${type.name}' provided by '${previous.javaClass.name}' " +
                            "was replaced by '${extension.javaClass.name}'.",
                    )
                }
            }
            return ApiExtensionRegistry(result)
        }
    }
}
