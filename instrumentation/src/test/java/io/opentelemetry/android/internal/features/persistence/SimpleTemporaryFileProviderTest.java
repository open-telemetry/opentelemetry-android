/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimpleTemporaryFileProviderTest {
    @TempDir File tempDir;

    @Test
    void createUniqueFilesBasedOnCurrentTimeAndPrefix() throws InterruptedException {
        SimpleTemporaryFileProvider provider = new SimpleTemporaryFileProvider(tempDir);
        File first = provider.createTemporaryFile("a");
        File second = provider.createTemporaryFile("b");
        Thread.sleep(1);
        File third = provider.createTemporaryFile("a");

        assertThat(first.getName()).startsWith("a").endsWith(".tmp");
        assertThat(second.getName()).startsWith("b").endsWith(".tmp");
        assertThat(third.getName()).startsWith("a").endsWith(".tmp");
        assertThat(first).isNotEqualTo(third);
    }
}
