/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.Incubating
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

@OptIn(Incubating::class)
@RunWith(AndroidJUnit4::class)
class FileSessionStorageTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private val first = SessionImpl("a".repeat(32), 100)
    private val second = SessionImpl("b".repeat(32), 200)

    @Test
    fun `new storage instance reads the complete replacement`() {
        val file = File(temporary.root, "session")
        val storage = SessionStorage.file(file)
        assertThat(storage.get().id).isEmpty()
        storage.save(first)
        assertThat(SessionStorage.file(file).get().id).isEqualTo(first.id)
        storage.save(second)
        val restored = SessionStorage.file(file).get()
        assertThat(restored.id).isEqualTo(second.id)
        assertThat(restored.startTimestamp).isEqualTo(second.startTimestamp)
    }

    @Test
    fun `interrupted replacement retains the previous record`() {
        val file = File(temporary.root, "session")
        SessionStorage.file(file).save(first)
        AtomicFile(file).startWrite().use { it.write(byteArrayOf(1, 2)) }
        assertThat(SessionStorage.file(file).get().id).isEqualTo(first.id)
    }

    @Test
    fun `truncated and invalid records cannot become a previous session`() {
        val file = File(temporary.root, "session")
        SessionStorage.file(file).save(first)
        val complete = file.readBytes()
        for (length in complete.indices) {
            file.writeBytes(complete.copyOf(length))
            assertThat(SessionStorage.file(file).get().id).describedAs("length %s", length).isEmpty()
        }
        for (bytes in listOf(complete + 0, complete.clone().apply { this[4] = '!'.code.toByte() })) {
            file.writeBytes(bytes)
            assertThat(SessionStorage.file(file).get().id).isEmpty()
        }
        file.writeBytes(complete.clone().apply { ByteBuffer.wrap(this).putLong(36, -1) })
        assertThat(SessionStorage.file(file).get().id).isEmpty()
    }

    @Test
    fun `unsupported versions survive reads and saves`() {
        val file = File(temporary.root, "session")
        val future = ByteBuffer.allocate(4).putInt(2).array()
        file.writeBytes(future)
        val storage = SessionStorage.file(file)
        assertThat(storage.get().id).isEmpty()
        storage.save(first)
        assertThat(storage.get().id).isEqualTo(first.id)
        assertThat(file.readBytes()).containsExactly(*future)
    }

    @Test
    fun `write failure retains memory and a later save retries`() {
        val parent = temporary.newFile("blocked")
        val file = File(parent, "session")
        val storage = SessionStorage.file(file)
        storage.save(first)
        assertThat(storage.get().id).isEqualTo(first.id)
        assertThat(parent.delete()).isTrue()
        storage.save(second)
        assertThat(SessionStorage.file(file).get().id).isEqualTo(second.id)
    }

    @Test
    fun `invalid sessions do not overwrite a valid record`() {
        val file = File(temporary.root, "session")
        val storage = SessionStorage.file(file)
        storage.save(first)
        storage.save(invalidSession)
        storage.save(SessionImpl("0".repeat(32), 100))
        storage.save(SessionImpl(second.id, -1))
        assertThat(SessionStorage.file(file).get().id).isEqualTo(first.id)
    }

    @Test
    fun `concurrent saves on one storage cannot mix record fields`() {
        val file = File(temporary.root, "session")
        val storage = SessionStorage.file(file)
        val executor = Executors.newFixedThreadPool(4)
        try {
            executor
                .invokeAll(List(30) { i -> Callable { storage.save(if (i % 2 == 0) first else second) } })
                .forEach { it.get(5, SECONDS) }
            val saved = SessionStorage.file(file).get()
            assertThat(saved.startTimestamp).isEqualTo(if (saved.id == first.id) first.startTimestamp else second.startTimestamp)
            assertThat(saved.id).isIn(first.id, second.id)
        } finally {
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue()
        }
    }
}
