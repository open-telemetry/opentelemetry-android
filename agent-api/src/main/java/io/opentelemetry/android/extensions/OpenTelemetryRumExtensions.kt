/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

import io.opentelemetry.android.OpenTelemetryRum

/** Returns the extension registered under [T], or null when absent. */
inline fun <reified T : ApiExtension> OpenTelemetryRum.extension(): T? = getExtension(T::class.java)
