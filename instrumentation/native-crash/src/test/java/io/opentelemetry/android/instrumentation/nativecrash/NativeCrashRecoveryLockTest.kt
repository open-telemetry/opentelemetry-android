/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.channels.FileChannel
import java.util.concurrent.TimeUnit

class NativeCrashRecoveryLockTest {
    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @AfterEach
    fun cleanup() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `another process waits until the recovery owner releases the lock`() {
        val store = FileNativeCrashStore(tempDir)
        var first = store.acquireRecoveryLock()
        assertThat(first).isNotNull()
        val ready = File(tempDir, "ready")
        val acquired = File(tempDir, "acquired")
        val output = File(tempDir, "child-output")
        val classpath =
            listOf(
                NativeCrashRecoveryLockProcess::class.java,
                FileNativeCrashStore::class.java,
                Unit::class.java,
            ).map { it.protectionDomain.codeSource.location }
                .map { File(it.toURI()).path }
                .distinct()
                .joinToString(File.pathSeparator)
        var process: Process? = null
        try {
            val child =
                ProcessBuilder(
                    File(System.getProperty("java.home"), "bin/java").path,
                    "-cp",
                    classpath,
                    NativeCrashRecoveryLockProcess::class.java.name,
                    tempDir.path,
                    ready.path,
                    acquired.path,
                ).redirectErrorStream(true).redirectOutput(output).start()
            process = child
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (!ready.exists() && child.isAlive && System.nanoTime() < deadline) Thread.sleep(10)
            assertThat(ready).describedAs(output.readText()).exists()
            assertThat(child.waitFor(200, TimeUnit.MILLISECONDS)).isFalse()
            assertThat(acquired).doesNotExist()

            first!!.close()
            first = null
            assertThat(child.waitFor(10, TimeUnit.SECONDS)).describedAs(output.readText()).isTrue()
            assertThat(child.exitValue()).describedAs(output.readText()).isZero()
            assertThat(acquired).hasContent("acquired")
            store.acquireRecoveryLock().also { assertThat(it).isNotNull() }!!.close()
        } finally {
            first?.close()
            process?.destroyForcibly()?.waitFor(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `lock creation failure does not consume crash files`() {
        val store = FileNativeCrashStore(tempDir)
        store.crashRecordPath.writeText("signal.number=11\n")
        File(tempDir, "native-crash-recovery.lock").mkdir()

        assertThat(store.acquireRecoveryLock()).isNull()
        assertThat(store.crashRecordPath).exists()
        assertThat(File(tempDir, "native-crash-recovery.lock").delete()).isTrue()
        checkNotNull(store.acquireRecoveryLock()).close()
    }

    @Test
    fun `uncreatable recovery directory fails without altering the blocking file`() {
        val blocker = File(tempDir, "blocked").apply { writeText("keep") }
        val store = FileNativeCrashStore(File(blocker, "native-crash"))

        assertThat(store.acquireRecoveryLock()).isNull()
        assertThat(blocker).hasContent("keep")
    }

    @Test
    fun `rejects an overlapping recovery lock in the same process`() {
        val first = FileNativeCrashStore(tempDir).acquireRecoveryLock()

        assertThat(first).isNotNull()
        assertThat(FileNativeCrashStore(tempDir).acquireRecoveryLock()).isNull()

        first!!.close()
        val next = FileNativeCrashStore(tempDir).acquireRecoveryLock()
        assertThat(next).isNotNull()
        next!!.close()
    }

    @Test
    fun `closing an old lock again does not release the next owner`() {
        val store = FileNativeCrashStore(tempDir)
        val first = checkNotNull(store.acquireRecoveryLock())
        first.close()
        checkNotNull(store.acquireRecoveryLock()).use {
            first.close()
            assertThat(store.acquireRecoveryLock()).isNull()
        }
        checkNotNull(store.acquireRecoveryLock()).close()
    }

    @Test
    fun `directory aliases share the same process owner`() {
        File(tempDir, "child").mkdir()
        checkNotNull(FileNativeCrashStore(tempDir).acquireRecoveryLock()).use {
            assertThat(FileNativeCrashStore(File(tempDir, "child/..")).acquireRecoveryLock()).isNull()
        }
    }

    @Test
    fun `an error while opening the channel releases the process owner`() {
        val failure = AssertionError("injected channel open error")
        val store = FileNativeCrashStore(tempDir, openRecoveryChannel = { throw failure })

        assertThatThrownBy { store.acquireRecoveryLock() }.isSameAs(failure)
        checkNotNull(FileNativeCrashStore(tempDir).acquireRecoveryLock()).close()
    }

    @Test
    fun `an error while locking closes the channel and releases the process owner`() {
        val failure = AssertionError("injected lock error")
        val channel = mockk<FileChannel>()
        var open = true
        every { channel.lock() } throws failure
        every { channel.isOpen } answers { open }
        every { channel.close() } answers { open = false }
        val store = FileNativeCrashStore(tempDir, openRecoveryChannel = { channel })

        assertThatThrownBy { store.acquireRecoveryLock() }.isSameAs(failure)
        verify(exactly = 1) { channel.close() }
        checkNotNull(FileNativeCrashStore(tempDir).acquireRecoveryLock()).close()
    }
}
