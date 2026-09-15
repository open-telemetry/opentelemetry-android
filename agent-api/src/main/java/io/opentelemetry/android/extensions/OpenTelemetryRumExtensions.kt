/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

import io.opentelemetry.android.OpenTelemetryRum

/**
 * Kotlin shorthand for [OpenTelemetryRum.getExtension].
 *
 * ```
 * openTelemetryRum.extension<SessionActivityApi>()?.userActive()
 * ```
 */
inline fun <reified T : ApiExtension> OpenTelemetryRum.extension(): T? = getExtension(T::class.java)
