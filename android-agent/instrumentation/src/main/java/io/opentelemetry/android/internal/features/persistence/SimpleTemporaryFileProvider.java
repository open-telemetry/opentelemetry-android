/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.persistence;

import io.opentelemetry.contrib.disk.buffering.internal.files.TemporaryFileProvider;
import java.io.File;
import java.util.UUID;

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
public final class SimpleTemporaryFileProvider implements TemporaryFileProvider {
    private final File tempDir;

    public SimpleTemporaryFileProvider(File tempDir) {
        this.tempDir = tempDir;
    }

    /** Creates a unique file instance using the provided prefix and the current time in millis. */
    @Override
    public File createTemporaryFile(String prefix) {
        return new File(tempDir, prefix + "_" + UUID.randomUUID() + ".tmp");
    }
}
