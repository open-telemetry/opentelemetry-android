/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.persistence;

import io.opentelemetry.android.config.DiskBufferingConfiguration;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.internal.services.CacheStorageService;
import io.opentelemetry.android.internal.services.PreferencesService;
import io.opentelemetry.android.internal.services.Service;
import io.opentelemetry.android.internal.services.ServiceManager;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
public final class DiskManager {
    private static final String MAX_FOLDER_SIZE_KEY = "max_signal_folder_size";
    private static final int MAX_FILE_SIZE = 1024 * 1024;
    private static final Logger logger = Logger.getLogger("DiskManager");
    private final CacheStorageService cacheStorageService;
    private final PreferencesService preferencesService;
    private final DiskBufferingConfiguration diskBufferingConfiguration;

    public static DiskManager create(OtelRumConfig config) {
        ServiceManager serviceManager = ServiceManager.get();
        return new DiskManager(
                serviceManager.getService(Service.Type.APPLICATION_INFO),
                serviceManager.getService(Service.Type.PREFERENCES),
                config.getDiskBufferingConfiguration());
    }

    DiskManager(
            CacheStorageService cacheStorageService,
            PreferencesService preferencesService,
            DiskBufferingConfiguration diskBufferingConfiguration) {
        this.cacheStorageService = cacheStorageService;
        this.preferencesService = preferencesService;
        this.diskBufferingConfiguration = diskBufferingConfiguration;
    }

    public File getSignalsBufferDir() throws IOException {
        File dir = new File(cacheStorageService.getCacheDir(), "opentelemetry/signals");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Could not create dir " + dir);
            }
        }
        return dir;
    }

    public File getTemporaryDir() throws IOException {
        File dir = new File(cacheStorageService.getCacheDir(), "opentelemetry/temp");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Could not create dir " + dir);
            }
        }
        deleteFiles(dir);
        return dir;
    }

    /**
     * It checks for the available cache space in disk, then it attempts to divide by 3 in order to
     * get each signal's folder max size. The resulting value is subtracted the max file size value
     * in order to account for temp files used during the reading process.
     *
     * <p>
     *
     * @return If the calculated size is < the max file size value, it returns 0. The calculated
     *     size is stored in the preferences and returned otherwise.
     */
    public int getMaxFolderSize() {
        int storedSize = preferencesService.retrieveInt(MAX_FOLDER_SIZE_KEY, -1);
        if (storedSize > 0) {
            logger.log(
                    Level.FINER,
                    String.format("Returning max folder size from preferences: %s", storedSize));
            return storedSize;
        }
        int requestedSize = diskBufferingConfiguration.maxCacheSize;
        int availableCacheSize = (int) cacheStorageService.ensureCacheSpaceAvailable(requestedSize);
        // Divides the available cache size by 3 (for each signal's folder) and then subtracts the
        // size of a single file to be aware of a temp file used when reading data back from the
        // disk.
        int calculatedSize = (availableCacheSize / 3) - MAX_FILE_SIZE;
        if (calculatedSize < MAX_FILE_SIZE) {
            logger.log(
                    Level.WARNING,
                    String.format(
                            "Insufficient folder cache size: %s, it must be at least: %s",
                            calculatedSize, MAX_FILE_SIZE));
            return 0;
        }
        preferencesService.store(MAX_FOLDER_SIZE_KEY, calculatedSize);

        logger.log(
                Level.FINER,
                String.format(
                        "Requested cache size: %s, available cache size: %s, folder size: %s",
                        requestedSize, availableCacheSize, calculatedSize));
        return calculatedSize;
    }

    public int getMaxCacheFileSize() {
        return MAX_FILE_SIZE;
    }

    private static void deleteFiles(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFiles(file);
                }
                file.delete();
            }
        }
    }
}
