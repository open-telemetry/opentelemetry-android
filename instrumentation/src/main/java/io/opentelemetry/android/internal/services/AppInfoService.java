/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services;

import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to get information about the host app.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public class AppInfoService implements Service {
    private final Context appContext;
    private final Logger logger = Logger.getLogger("AppInfoService");

    public AppInfoService(Context appContext) {
        this.appContext = appContext;
    }

    public File getCacheDir() {
        return appContext.getCacheDir();
    }

    @WorkerThread
    public long getAvailableCacheSpace(long maxSpaceNeeded) {
        File cacheDir = getCacheDir();
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return getLegacyAvailableSpace(cacheDir, maxSpaceNeeded);
        }
        return getAvailableSpace(cacheDir, maxSpaceNeeded);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private long getAvailableSpace(File directory, long maxSpaceNeeded) {
        logger.log(
                Level.FINER,
                String.format(
                        "Getting available space for %s, max needed is: %s",
                        directory, maxSpaceNeeded));
        try {
            StorageManager storageManager = appContext.getSystemService(StorageManager.class);
            UUID appSpecificInternalDirUuid = storageManager.getUuidForPath(directory);
            // Get the minimum amount of allocatable space.
            long spaceToAllocate =
                    Math.min(
                            storageManager.getAllocatableBytes(appSpecificInternalDirUuid),
                            maxSpaceNeeded);
            // Ensure the space is available by asking the OS to clear stale cache if needed.
            storageManager.allocateBytes(appSpecificInternalDirUuid, spaceToAllocate);
            return spaceToAllocate;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to get available space", e);
            return getLegacyAvailableSpace(directory, maxSpaceNeeded);
        }
    }

    private long getLegacyAvailableSpace(File directory, long maxSpaceNeeded) {
        logger.log(
                Level.FINER,
                String.format(
                        "Getting legacy available space for %s max needed is: %s",
                        directory, maxSpaceNeeded));
        return Math.min(directory.getUsableSpace(), maxSpaceNeeded);
    }

    @Override
    public void start() {
        // No operation.
    }

    @Override
    public void stop() {
        // No operation.
    }

    @Override
    public Type type() {
        return Type.APPLICATION_INFO;
    }
}
