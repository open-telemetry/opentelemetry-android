/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.persistence

import io.opentelemetry.contrib.disk.buffering.internal.files.TemporaryFileProvider
import java.io.File
import java.util.UUID

internal class SimpleTemporaryFileProvider(private val tempDir: File) : TemporaryFileProvider {
    /** Creates a unique file instance using the provided prefix and the current time in millis.  */
    override fun createTemporaryFile(prefix: String): File {
        return File(tempDir, prefix + "_" + UUID.randomUUID() + ".tmp")
    }
}
