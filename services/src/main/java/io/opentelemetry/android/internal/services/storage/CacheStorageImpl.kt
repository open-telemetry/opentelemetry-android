/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.storage

import android.content.Context
import java.io.File

/**
 * Utility to get information about the host app.
 *
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
internal class CacheStorageImpl(
    private val appContext: Context,
) : CacheStorage {
    override val cacheDir: File
        get() = appContext.cacheDir
}
