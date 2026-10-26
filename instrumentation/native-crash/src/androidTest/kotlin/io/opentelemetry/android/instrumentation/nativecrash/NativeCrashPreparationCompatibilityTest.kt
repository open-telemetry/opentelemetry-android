/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NativeCrashPreparationCompatibilityTest {
    @Before
    fun loadLibrary() {
        System.loadLibrary("otel_android_native_crash")
    }

    @Test
    fun preparesHeaderModulesAndPathsBeforePublishing() {
        assertThat(NativeCrashPreparationTestJni.check(0)).isTrue()
    }

    @Test
    fun failedInstallationStaysUnpublishedAndCanRetry() {
        assertThat(NativeCrashPreparationTestJni.check(1)).isTrue()
    }

    @Test
    fun moduleFailureStillInstallsMarkerCapture() {
        assertThat(NativeCrashPreparationTestJni.check(2)).isTrue()
    }

    @Test
    fun missingEmptyAndOversizedPathsStillInstallMarkerCapture() {
        assertThat(NativeCrashPreparationTestJni.check(3)).isTrue()
    }

    @Test
    fun acceptsTheLongestPathThatLeavesSpaceForTemporarySuffix() {
        assertThat(NativeCrashPreparationTestJni.check(4)).isTrue()
    }

    @Test
    fun publishedStateCannotBeOverwritten() {
        assertThat(NativeCrashPreparationTestJni.check(5)).isTrue()
    }

    @Test
    fun jniInstallationPreparesOnceWithoutWritingASnapshot() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val directory = File(context.cacheDir, "native-preparation").apply { mkdirs() }
        val marker = File(directory, "native-crash-record.properties")
        val snapshot = File(directory, "native-crash-snapshot.bin")
        assertThat(NativeCrashJni.installForPaths(marker.absolutePath, snapshot.absolutePath)).isTrue()
        val prepared = requireNotNull(NativeCrashPreparationTestJni.preparedSnapshot())
        assertThat(prepared).hasSize(NativeCrashSnapshotLayout.RECORD_SIZE)
        assertThat(NativeCrashJni.installForPaths(marker.absolutePath, snapshot.absolutePath)).isTrue()
        assertThat(NativeCrashJni.installForPaths(marker.absolutePath, "/different-snapshot")).isFalse()
        assertThat(NativeCrashJni.installForPaths("/different-marker", snapshot.absolutePath)).isFalse()
        assertThat(NativeCrashPreparationTestJni.preparedSnapshot()).isEqualTo(prepared)
        assertThat(marker).doesNotExist()
        assertThat(snapshot).doesNotExist()
        assertThat(File(snapshot.path + ".tmp")).doesNotExist()
    }
}

internal object NativeCrashPreparationTestJni {
    @JvmStatic external fun check(scenario: Int): Boolean

    @JvmStatic external fun preparedSnapshot(): ByteArray?
}
