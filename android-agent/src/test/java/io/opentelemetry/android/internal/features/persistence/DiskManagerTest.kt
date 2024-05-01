/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.android.internal.features.persistence

import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration
import io.opentelemetry.android.internal.services.CacheStorageService
import io.opentelemetry.android.internal.services.PreferencesService
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class DiskManagerTest {
    @MockK
    lateinit var cacheStorageService: CacheStorageService

    @MockK
    lateinit var preferencesService: PreferencesService

    @MockK
    lateinit var diskBufferingConfiguration: DiskBufferingConfiguration

    @TempDir
    lateinit var cacheDir: File
    private lateinit var diskManager: DiskManager

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { cacheStorageService.cacheDir }.returns(cacheDir)
        diskManager =
            DiskManager(cacheStorageService, preferencesService, diskBufferingConfiguration)
    }

    @Test
    fun `provides the signal buffer dir`() {
        val expected = File(cacheDir, "opentelemetry/signals")
        assertThat(diskManager.signalsBufferDir).isEqualTo(expected)
        assertThat(expected.exists()).isTrue()
    }

    @Test
    fun `provides a temp dir`() {
        every { cacheStorageService.cacheDir }.returns(cacheDir)
        val expected = File(cacheDir, "opentelemetry/temp")
        assertThat(diskManager.temporaryDir).isEqualTo(expected)
        assertThat(expected.exists()).isTrue()
    }

    @Test
    fun `cleans up the temp dir before providing it`() {
        val dir = File(cacheDir, "opentelemetry/temp")
        assertTrue(dir.mkdirs())
        assertTrue(File(dir, "somefile.tmp").createNewFile())
        assertTrue(File(dir, "some_other_file.tmp").createNewFile())
        assertTrue(File(dir, "somedir").mkdirs())
        assertTrue(File(dir, "somedir/some_other_file.tmp").createNewFile())
        val temporaryDir = diskManager.temporaryDir
        assertThat(temporaryDir.exists()).isTrue()
        assertThat(temporaryDir.listFiles()?.size ?: -1).isZero()
    }

    @Test
    fun `can get the max cache file size`() {
        val persistenceSize = 1024 * 1024 * 2
        every { diskBufferingConfiguration.maxCacheFileSize }.returns(persistenceSize)
        assertThat(diskManager.maxCacheFileSize).isEqualTo(persistenceSize)
        verify {
            diskBufferingConfiguration.maxCacheFileSize
        }
    }

    @Test
    fun `can get max signal folder size`() {
        val maxCacheSize = (10 * 1024 * 1024).toLong() // 10 MB
        val maxCacheFileSize = 1024 * 1024 // 1 MB
        every { diskBufferingConfiguration.maxCacheSize }.returns(maxCacheSize.toInt())
        every { diskBufferingConfiguration.maxCacheFileSize }.returns(maxCacheFileSize)
        every { cacheStorageService.ensureCacheSpaceAvailable(maxCacheSize)}.returns(maxCacheSize)
        every { preferencesService.retrieveInt(MAX_FOLDER_SIZE_KEY, -1)}.returns(-1)
        every { preferencesService.store(any(), any())} just Runs

        // Expects the size of a single signal type folder minus the size of a cache file, to use as
        // temporary space for reading.
        val expected = 2446677
        assertThat(diskManager.maxFolderSize).isEqualTo(expected)
        verify {
            preferencesService.store(MAX_FOLDER_SIZE_KEY, expected)
        }

        // On a second call, should get the value from the preferences.
        clearMocks(cacheStorageService, diskBufferingConfiguration, preferencesService)
        every { preferencesService.retrieveInt(MAX_FOLDER_SIZE_KEY, -1)}.returns(expected)
        assertThat(diskManager.maxFolderSize).isEqualTo(expected)

        verify {
            preferencesService.retrieveInt(MAX_FOLDER_SIZE_KEY, -1)
        }
        confirmVerified(preferencesService)
        verify { cacheStorageService wasNot Called}
        verify { diskBufferingConfiguration wasNot Called}
    }

    @Test
    fun `max folder size is used when calculated size is invalid`() {
        val maxCacheSize = (1024 * 1024).toLong() // 1 MB
        val maxCacheFileSize = 1024 * 1024 // 1 MB
        every { diskBufferingConfiguration.maxCacheSize }.returns(maxCacheSize.toInt())
        every { diskBufferingConfiguration.maxCacheFileSize }.returns(maxCacheFileSize)
        every { cacheStorageService.ensureCacheSpaceAvailable(maxCacheSize)}.returns(maxCacheSize)
        every { preferencesService.retrieveInt(MAX_FOLDER_SIZE_KEY, -1)}.returns(-1)
        // Expects the size of a single signal type folder minus the size of a cache file, to use as
        // temporary space for reading.
        assertThat(diskManager.maxFolderSize).isEqualTo(0)
        verify(inverse=true) {
            preferencesService.store(any(), any())
        }
    }

    companion object {
        private const val MAX_FOLDER_SIZE_KEY = "max_signal_folder_size"
    }
}
