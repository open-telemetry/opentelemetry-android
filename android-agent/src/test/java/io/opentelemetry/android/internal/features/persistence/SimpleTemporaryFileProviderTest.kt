/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.android.internal.features.persistence

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class SimpleTemporaryFileProviderTest {
    @TempDir
    lateinit var tempDir: File
    @Test
    fun createUniqueFilesBasedOnCurrentTimeAndPrefix() {
        val provider = SimpleTemporaryFileProvider(tempDir)
        val first = provider.createTemporaryFile("a")
        val second = provider.createTemporaryFile("b")
        Thread.sleep(1)
        val third = provider.createTemporaryFile("a")
        assertThat(first.getName()).startsWith("a").endsWith(".tmp")
        assertThat(second.getName()).startsWith("b").endsWith(".tmp")
        assertThat(third.getName()).startsWith("a").endsWith(".tmp")
        assertThat(first).isNotEqualTo(third)
        assertThat(first.getParentFile()).isEqualTo(tempDir)
        assertThat(second.getParentFile()).isEqualTo(tempDir)
        assertThat(third.getParentFile()).isEqualTo(tempDir)
    }
}
