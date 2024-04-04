/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration;
import io.opentelemetry.android.internal.services.CacheStorageAppWorker;
import io.opentelemetry.android.internal.services.PreferencesAppWorker;
import io.opentelemetry.android.internal.services.AppWorkerManager;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiskManagerTest {

    private static final String MAX_FOLDER_SIZE_KEY = "max_signal_folder_size";
    @Mock
    CacheStorageAppWorker cacheStorageService;
    @Mock
    PreferencesAppWorker preferencesService;
    @Mock DiskBufferingConfiguration diskBufferingConfiguration;
    @TempDir File cacheDir;
    private DiskManager diskManager;

    @BeforeEach
    void setUp() {
        AppWorkerManager appWorkerManager = mock();
        doReturn(cacheStorageService).when(appWorkerManager).getService(CacheStorageAppWorker.class);
        doReturn(preferencesService).when(appWorkerManager).getService(PreferencesAppWorker.class);
        AppWorkerManager.setForTest(appWorkerManager);
        diskManager = DiskManager.create(diskBufferingConfiguration);
    }

    @Test
    void provideSignalBufferDir() throws IOException {
        doReturn(cacheDir).when(cacheStorageService).getCacheDir();
        File expected = new File(cacheDir, "opentelemetry/signals");

        assertEquals(expected, diskManager.getSignalsBufferDir());
        assertTrue(expected.exists());
    }

    @Test
    void provideTemporaryDir() throws IOException {
        doReturn(cacheDir).when(cacheStorageService).getCacheDir();
        File expected = new File(cacheDir, "opentelemetry/temp");

        assertEquals(expected, diskManager.getTemporaryDir());
        assertTrue(expected.exists());
    }

    @Test
    void cleanupTemporaryDirBeforeProvidingIt() throws IOException {
        File dir = new File(cacheDir, "opentelemetry/temp");
        assertTrue(dir.mkdirs());
        assertTrue(new File(dir, "somefile.tmp").createNewFile());
        assertTrue(new File(dir, "some_other_file.tmp").createNewFile());
        assertTrue(new File(dir, "somedir").mkdirs());
        assertTrue(new File(dir, "somedir/some_other_file.tmp").createNewFile());

        File temporaryDir = diskManager.getTemporaryDir();

        assertTrue(temporaryDir.exists());
        assertEquals(0, Objects.requireNonNull(temporaryDir.listFiles()).length);
    }

    @Test
    void getMaxCacheFileSize() {
        int persistenceSize = 1024 * 1024 * 2;
        doReturn(persistenceSize).when(diskBufferingConfiguration).getMaxCacheFileSize();

        assertEquals(persistenceSize, diskManager.getMaxCacheFileSize());

        verify(diskBufferingConfiguration).getMaxCacheFileSize();
    }

    @Test
    void getMaxSignalFolderSize() {
        long maxCacheSize = 10 * 1024 * 1024; // 10 MB
        int maxCacheFileSize = 1024 * 1024; // 1 MB
        doReturn((int) maxCacheSize).when(diskBufferingConfiguration).getMaxCacheSize();
        doReturn(maxCacheFileSize).when(diskBufferingConfiguration).getMaxCacheFileSize();
        doReturn(maxCacheSize).when(cacheStorageService).ensureCacheSpaceAvailable(maxCacheSize);
        doReturn(-1).when(preferencesService).retrieveInt(MAX_FOLDER_SIZE_KEY, -1);

        // Expects the size of a single signal type folder minus the size of a cache file, to use as
        // temporary space for reading.
        int expected = 2_446_677;
        assertEquals(expected, diskManager.getMaxFolderSize());
        verify(preferencesService).store(MAX_FOLDER_SIZE_KEY, expected);

        // On a second call, should get the value from the preferences.
        clearInvocations(cacheStorageService, diskBufferingConfiguration, preferencesService);
        doReturn(expected).when(preferencesService).retrieveInt(MAX_FOLDER_SIZE_KEY, -1);
        assertEquals(expected, diskManager.getMaxFolderSize());
        verify(preferencesService).retrieveInt(MAX_FOLDER_SIZE_KEY, -1);
        verifyNoMoreInteractions(preferencesService);
        verifyNoInteractions(cacheStorageService, diskBufferingConfiguration);
    }

    @Test
    void getMaxSignalFolderSize_whenCalculatedSizeIsNotValid() {
        long maxCacheSize = 1024 * 1024; // 1 MB
        int maxCacheFileSize = 1024 * 1024; // 1 MB
        doReturn((int) maxCacheSize).when(diskBufferingConfiguration).getMaxCacheSize();
        doReturn(maxCacheFileSize).when(diskBufferingConfiguration).getMaxCacheFileSize();
        doReturn(maxCacheSize).when(cacheStorageService).ensureCacheSpaceAvailable(maxCacheSize);
        doReturn(-1).when(preferencesService).retrieveInt(MAX_FOLDER_SIZE_KEY, -1);

        // Expects the size of a single signal type folder minus the size of a cache file, to use as
        // temporary space for reading.
        int expected = 0;
        assertEquals(expected, diskManager.getMaxFolderSize());
        verify(preferencesService, never()).store(MAX_FOLDER_SIZE_KEY, expected);
    }
}
