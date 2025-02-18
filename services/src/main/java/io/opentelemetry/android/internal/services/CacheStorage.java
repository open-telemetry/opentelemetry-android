/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services;

import android.content.Context;
import java.io.File;

/**
 * Utility to get information about the host app.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public class CacheStorage {
    private final Context appContext;

    public CacheStorage(Context appContext) {
        this.appContext = appContext;
    }

    public File getCacheDir() {
        return appContext.getCacheDir();
    }
}
