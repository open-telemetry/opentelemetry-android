/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.UUID

class NativeCrashStorageTest {
    @TempDir
    lateinit var root: File

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `keeps consecutive launches and their context separate`() {
        val first = storage(1)
        val second = storage(2)
        val third = storage(3)
        listOf(first, second, third).forEachIndexed { index, storage ->
            crash(storage.currentStore, "session-$index")
        }
        val sessions = mutableListOf<String?>()
        third.replayPreviousCrashes { store ->
            sessions += store.readContext()?.sessionId
            assertThat(store.readCrashRecord()?.signalNumber).isEqualTo(11)
            store.deleteCrashFiles()
        }
        assertThat(sessions).containsExactly("session-1", "session-0")
        assertThat(first.currentStore.crashRecordPath.parentFile).doesNotExist()
        assertThat(second.currentStore.crashRecordPath.parentFile).doesNotExist()
        assertThat(third.currentStore.readContext()?.sessionId).isEqualTo("session-2")
        assertThat(third.currentStore.readCrashRecord()).isNotNull()
        third.replayPreviousCrashes { error("Completed reports must not replay") }
    }

    @Test
    fun `replays legacy files in place without replacing context or recovery state`() {
        val legacy = FileNativeCrashStore(root)
        crash(legacy, "legacy")
        val state = File(root, "native-crash-recovery.properties").apply { writeText("delivery claim") }
        val current = storage(1)
        crash(current.currentStore, "current")
        current.replayPreviousCrashes { store ->
            assertThat(store.crashRecordPath).isEqualTo(legacy.crashRecordPath.canonicalFile)
            assertThat(store.readContext()?.sessionId).isEqualTo("legacy")
            assertThat(state.readText()).isEqualTo("delivery claim")
            store.deleteCrashFiles()
            state.delete()
        }
        assertThat(current.currentStore.readCrashRecord()).isNotNull()
        current.replayPreviousCrashes { error("Legacy report must not replay twice") }
    }

    @Test
    fun `keeps pending recovery and continues past a failed report`() {
        val first = storage(1)
        val second = storage(2)
        crash(first.currentStore, "first")
        crash(second.currentStore, "second")
        val current = storage(3)
        val visited = mutableListOf<File>()
        current.replayPreviousCrashes {
            visited += it.crashRecordPath
            if (it.crashRecordPath == second.currentStore.crashRecordPath) throw IOException("unreadable")
            it.deleteCrashFiles()
        }
        assertThat(visited).containsExactly(second.currentStore.crashRecordPath, first.currentStore.crashRecordPath)
        assertThat(second.currentStore.crashRecordPath).exists()
        val retry = mutableListOf<File>()
        current.replayPreviousCrashes { retry += it.crashRecordPath }
        assertThat(retry).containsExactly(second.currentStore.crashRecordPath)
    }

    @Test
    fun `prunes oldest launches without touching current or unrelated files`() {
        val launches = (1..10).map { storage(it).also { storage -> crash(storage.currentStore, "session-$it") } }
        val unrelated = File(root, "other-feature").apply { mkdir() }
        val current = storage(11)
        crash(current.currentStore, "active")
        val visited = mutableListOf<File>()
        current.replayPreviousCrashes { visited += it.crashRecordPath }
        assertThat(visited).containsExactlyElementsOf(launches.drop(2).reversed().map { it.currentStore.crashRecordPath })
        launches.take(2).forEach { assertThat(it.currentStore.crashRecordPath.parentFile).doesNotExist() }
        assertThat(current.currentStore.readCrashRecord()).isNotNull()
        assertThat(unrelated).isDirectory()
    }

    @Test
    fun `removes context-only launches but retains pending recovery state`() {
        val empty = storage(1)
        empty.currentStore.writeContext(context("empty"))
        val pending = storage(2)
        pending.currentStore.writeContext(context("pending"))
        val state = File(pending.currentStore.crashRecordPath.parentFile, "native-crash-recovery.properties")
        state.writeText("pending cleanup")
        storage(3).replayPreviousCrashes { }
        assertThat(empty.currentStore.crashRecordPath.parentFile).doesNotExist()
        assertThat(state).exists()
    }

    @Test
    fun `does not follow launch symlinks or delete unrecognized contents`() {
        val outside = File(root, "outside").apply { mkdir() }
        val marker = File(outside, "native-crash-record.properties").apply { writeText("preserve") }
        Files.createSymbolicLink(File(root, launchName(1)).toPath(), outside.toPath())
        val current = storage(2)
        current.replayPreviousCrashes { error("Must not follow symbolic link") }
        assertThat(marker.readText()).isEqualTo("preserve")
        val unexpected = File(storage(3).currentStore.crashRecordPath.parentFile, "nested").apply { mkdirs() }
        val nested = File(unexpected, "preserve").apply { writeText("keep") }
        current.replayPreviousCrashes { }
        assertThat(nested.readText()).isEqualTo("keep")
    }

    @Test
    fun `uses the same active directory for repeated installation and path aliases`() {
        val first = NativeCrashStorage(root)
        crash(first.currentStore, "active")
        val alias = NativeCrashStorage(File(root, "child/.."))
        assertThat(alias.currentStore.crashRecordPath).isEqualTo(first.currentStore.crashRecordPath)
        alias.replayPreviousCrashes { error("The active launch must not replay") }
    }

    private fun storage(number: Int) = NativeCrashStorage(root, launchName(number))

    private fun launchName(number: Int) = "launch-${number.toString().padStart(19, '0')}-${UUID.randomUUID()}"

    private fun context(session: String) = NativeCrashContext(session, "1", "Android", "29")

    private fun crash(
        store: NativeCrashStore,
        session: String,
    ) {
        assertThat(store.writeContext(context(session))).isTrue()
        store.crashRecordPath.writeText("signal.number=11\ntimestamp.epoch_nanos=1000000000\n")
        store.crashSnapshotPath.writeText("partial snapshot")
    }
}
