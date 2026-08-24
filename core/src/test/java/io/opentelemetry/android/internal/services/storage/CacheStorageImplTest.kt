/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.storage

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.Test
import java.io.File

class CacheStorageImplTest {
    @Test
    fun `check cacheDir is cached and calls appContext cacheDir only once`() {
        val context = mockk<Context>()
        val expectedDir = File("/mock/cache/dir")
        every { context.cacheDir } returns expectedDir

        val cacheStorage = CacheStorageImpl(context)

        // Read cacheDir 3 times and ensure the same value is returned each time.
        repeat(3) {
            assertThat(cacheStorage.cacheDir).isEqualTo(expectedDir)
        }

        // The underlying context cacheDir should only be queried once.
        verify(exactly = 1) { context.cacheDir }
    }
}
