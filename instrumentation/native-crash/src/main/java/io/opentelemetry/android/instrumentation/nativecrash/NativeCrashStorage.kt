/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import android.util.Log
import io.opentelemetry.android.common.RumConstants
import java.io.File
import java.io.IOException
import java.util.UUID

/** Keeps this process's capture files separate from reports awaiting replay. Single-process only. */
internal class NativeCrashStorage(
    directory: File,
    launchName: String = processLaunchName,
) {
    private val directory = directory.canonicalFile
    private val currentDirectory = File(this.directory, launchName)
    val currentStore: NativeCrashStore = FileNativeCrashStore(currentDirectory)

    init {
        require(launchPattern.matches(launchName))
        require(currentDirectory.canonicalFile == currentDirectory) { "Crash directory must not be a symbolic link" }
    }

    fun replayPreviousCrashes(replay: (NativeCrashStore) -> Unit) {
        // Repeated SDK installation must not replay the same report concurrently within this process.
        synchronized(replayLock) {
            // The legacy flat layout stays in place, so an interrupted migration cannot split a report.
            if (hasReport(directory)) replaySafely(directory, replay)
            val previous =
                runCatching { directory.listFiles() }
                    .onFailure { error ->
                        Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to list native crash directories", error)
                    }.getOrNull()
                    ?.filter { it != currentDirectory && launchPattern.matches(it.name) && isLaunchDirectory(it) }
                    ?.sortedByDescending { it.name }
                    ?: return
            previous.drop(MAX_PREVIOUS_LAUNCHES).forEach(::deleteLaunch)
            previous.take(MAX_PREVIOUS_LAUNCHES).forEach { launch ->
                replaySafely(launch, replay)
                if (!hasReport(launch)) deleteLaunch(launch)
            }
        }
    }

    private fun isLaunchDirectory(launch: File): Boolean =
        runCatching { launch.isDirectory && launch.canonicalFile == launch }.getOrDefault(false)

    private fun replaySafely(
        launch: File,
        replay: (NativeCrashStore) -> Unit,
    ) {
        try {
            replay(FileNativeCrashStore(launch))
        } catch (error: Exception) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to replay native crash directory", error)
        } catch (error: LinkageError) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to replay native crash directory", error)
        }
    }

    private fun hasReport(launch: File): Boolean = runCatching { reportFiles.any { File(launch, it).exists() } }.getOrDefault(true)

    private fun deleteLaunch(launch: File) {
        try {
            val files = launch.listFiles() ?: return
            // Never traverse a nested directory or follow a link while pruning old launches.
            if (files.any { it.name !in launchFiles || !it.isFile || it.canonicalFile != it }) return
            if (!files.map { it.delete() }.all { it } || !launch.delete()) {
                throw IOException("Failed to remove native crash directory")
            }
        } catch (error: Exception) {
            Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Failed to prune native crash directory", error)
        }
    }

    private companion object {
        const val MAX_PREVIOUS_LAUNCHES = 8
        val processLaunchName = "launch-${System.currentTimeMillis().coerceAtLeast(0).toString().padStart(19, '0')}-${UUID.randomUUID()}"
        val launchPattern = Regex("launch-[0-9]{19}-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        val replayLock = Any()
        val reportFiles = setOf("native-crash-record.properties", "native-crash-snapshot.bin", "native-crash-recovery.properties")
        val launchFiles =
            reportFiles +
                setOf(
                    "native-crash-record.properties.tmp",
                    "native-crash-context.properties",
                    "native-crash-context.properties.tmp",
                    "native-crash-recovery.properties.tmp",
                )
    }
}
