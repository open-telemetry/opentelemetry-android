package com.splunk.rum;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)
public class DeviceSpanStorageLimiterTest {

    private static final int MAX_STORAGE_USE_MB = 3;
    private static final long MAX_STORAGE_USE_BYTES = MAX_STORAGE_USE_MB * 1024 * 1024;
    @Mock
    private FileUtils fileUtils;
    @Mock
    private File path;
    private DeviceSpanStorageLimiter limiter;

    @Before
    public void setup(){
        limiter = DeviceSpanStorageLimiter.builder()
                .fileUtils(fileUtils)
                .path(path)
                .maxStorageUseMb(MAX_STORAGE_USE_MB)
                .build();
    }

    @Test
    public void ensureFreeSpace_littleUsageEnoughFreeSpace() {
        when(fileUtils.getTotalFileSizeInBytes(path)).thenReturn(10 * 1024L);
        when(path.getFreeSpace()).thenReturn(99L);// Disk is very full
        assertFalse(limiter.ensureFreeSpace());
        verify(fileUtils, never()).safeDelete(any());
    }

    @Test
    public void ensureFreeSpace_littleUsageButNotEnoughFreeSpace() {
        when(fileUtils.getTotalFileSizeInBytes(path)).thenReturn(10 * 1024L);
        when(path.getFreeSpace()).thenReturn(MAX_STORAGE_USE_BYTES*99);// lots of room
        assertTrue(limiter.ensureFreeSpace());
        verify(fileUtils, never()).safeDelete(any());
    }

    @Test
    public void ensureFreeSpace_underLimit() {
        when(fileUtils.getTotalFileSizeInBytes(path)).thenReturn(MAX_STORAGE_USE_BYTES - 1);
        when(path.getFreeSpace()).thenReturn(MAX_STORAGE_USE_BYTES + 1);
        boolean result = limiter.ensureFreeSpace();
        assertTrue(result);
        verify(fileUtils, never()).safeDelete(any());
    }

    @Test
    public void ensureFreeSpace_overLimitHappyDeletion() {
        File file1 = new File("oldest");
        File file2 = new File("younger");
        File file3 = new File("newest");

        when(fileUtils.getTotalFileSizeInBytes(path)).thenReturn(MAX_STORAGE_USE_BYTES + 1);
        when(fileUtils.getModificationTime(file1)).thenReturn(1000L);
        when(fileUtils.getModificationTime(file2)).thenReturn(1001L);
        when(fileUtils.getModificationTime(file3)).thenReturn(1002L);
        when(fileUtils.getFileSize(isA(File.class))).thenReturn(1L);
        when(fileUtils.listSpanFiles(path)).thenReturn(Stream.of(file3, file1, file2));
        when(path.getFreeSpace()).thenReturn(MAX_STORAGE_USE_BYTES + 1);

        boolean result = limiter.ensureFreeSpace();

        verify(fileUtils).safeDelete(file1);
        verify(fileUtils).safeDelete(file2);
        verify(fileUtils, never()).safeDelete(file3);
        assertTrue(result);
    }



}