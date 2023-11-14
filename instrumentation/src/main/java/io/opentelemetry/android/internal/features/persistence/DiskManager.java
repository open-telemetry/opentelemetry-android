/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.persistence;

import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.config.PersistenceConfiguration;
import io.opentelemetry.android.internal.services.AppInfoService;
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
    private final AppInfoService appInfoService;
    private final PreferencesService preferencesService;
    private final PersistenceConfiguration persistenceConfiguration;
    private static final String MAX_FOLDER_SIZE_KEY = "max_signal_folder_size";
    private static final int MAX_FILE_SIZE = 1024 * 1024;
    private final static Logger logger = Logger.getLogger("DiskManager");

    public static DiskManager create(OtelRumConfig config) {
        ServiceManager serviceManager = ServiceManager.get();
        return new DiskManager(
                serviceManager.getService(Service.Type.APPLICATION_INFO),
                serviceManager.getService(Service.Type.PREFERENCES),
                config.getPersistenceConfiguration());
    }

    DiskManager(
            AppInfoService appInfoService,
            PreferencesService preferencesService,
            PersistenceConfiguration persistenceConfiguration) {
        this.appInfoService = appInfoService;
        this.preferencesService = preferencesService;
        this.persistenceConfiguration = persistenceConfiguration;
    }

    public File getSignalsCacheDir() throws IOException {
        File dir = new File(appInfoService.getCacheDir(), "opentelemetry/signals");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Could not create dir " + dir);
            }
        }
        return dir;
    }

    public File getTemporaryDir() throws IOException {
        File dir = new File(appInfoService.getCacheDir(), "opentelemetry/temp");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Could not create dir " + dir);
            }
        }
        deleteFiles(dir);
        return dir;
    }

    public int getMaxFolderSize() {
        int storedSize = preferencesService.retrieveInt(MAX_FOLDER_SIZE_KEY, -1);
        if (storedSize != -1) {
            logger.log(
                    Level.FINER,
                    String.format("Returning max folder size from preferences: %s", storedSize));
            return storedSize;
        }
        int requestedSize = persistenceConfiguration.maxCacheSize;
        int availableCacheSize = (int) appInfoService.getAvailableCacheSpace(requestedSize);
        int calculatedSize = (availableCacheSize / 3) - MAX_FILE_SIZE;
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
