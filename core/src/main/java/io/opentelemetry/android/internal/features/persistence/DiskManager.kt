/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.persistence

import android.util.Log
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.android.internal.services.CacheStorage
import java.io.File
import java.io.IOException

internal class DiskManager(
    private val cacheStorage: CacheStorage,
    private val diskBufferingConfig: DiskBufferingConfig,
) {
    @get:Throws(IOException::class)
    val signalsBufferDir: File
        get() {
            val dir = diskBufferingConfig.signalsBufferDir ?: File(cacheStorage.cacheDir, "opentelemetry/signals")
            ensureExistingOrThrow(dir)
            return dir
        }

    @get:Throws(IOException::class)
    val temporaryDir: File
        get() {
            val dir = File(cacheStorage.cacheDir, "opentelemetry/temp")
            ensureExistingOrThrow(dir)
            deleteFiles(dir)
            return dir
        }

    val maxFolderSize: Int
        /**
         * It divides the requested cache size by 3 in order to
         * get each signal's folder max size.
         *
         * @return The calculated size is stored in the preferences and returned.
         */
        get() {
            val requestedSize = diskBufferingConfig.maxCacheSize

            // Divides the available cache size by 3 (for each signal's folder)
            val calculatedSize = requestedSize / 3
            Log.d(
                RumConstants.OTEL_RUM_LOG_TAG,
                String.format(
                    "Requested cache size: %s, folder size: %s",
                    requestedSize,
                    calculatedSize,
                ),
            )
            return calculatedSize
        }

    val maxCacheFileSize: Int
        get() = diskBufferingConfig.maxCacheFileSize

    companion object {
        private fun deleteFiles(dir: File) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory()) {
                        deleteFiles(file)
                    }
                    file.delete()
                }
            }
        }

        private fun ensureExistingOrThrow(dir: File) {
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw IOException("Could not create dir $dir")
                }
            }
        }
    }
}
