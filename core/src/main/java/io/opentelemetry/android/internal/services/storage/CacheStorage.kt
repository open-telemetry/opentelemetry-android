/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.storage

import io.opentelemetry.android.internal.services.Service
import java.io.File

internal interface CacheStorage : Service {
    val cacheDir: File
}
