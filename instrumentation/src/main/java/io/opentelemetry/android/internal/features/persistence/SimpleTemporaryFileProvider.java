/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.persistence;

import io.opentelemetry.contrib.disk.buffering.internal.files.TemporaryFileProvider;
import java.io.File;

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
public final class SimpleTemporaryFileProvider implements TemporaryFileProvider {
    private final File tempDir;

    public SimpleTemporaryFileProvider(File tempDir) {
        this.tempDir = tempDir;
    }

    @Override
    public File createTemporaryFile(String prefix) {
        return new File(tempDir, prefix + "_" + System.currentTimeMillis() + ".tmp");
    }
}
