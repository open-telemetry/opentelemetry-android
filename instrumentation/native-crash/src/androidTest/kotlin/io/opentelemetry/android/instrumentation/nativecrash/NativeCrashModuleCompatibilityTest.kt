/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class NativeCrashModuleCompatibilityTest {
    private val crash = NativeCrashRecord(11, Instant.ofEpochSecond(1))

    @Before
    fun loadLibrary() {
        System.loadLibrary("otel_android_native_crash")
    }

    @Test
    fun executableSegmentsKeepLoaderOrderWithAppLibrariesFirst() {
        val modules = modules(0)
        assertThat(modules.map { it.name }).containsExactly("libapp.so", "libadopted.so", "libsystem.so", "libapex.so")
        assertThat(modules.map { it.loadBias }).containsExactly(0x20000UL, 0x40000UL, 0x10000UL, 0x30000UL)
        modules.forEach {
            assertThat(it.executableStart).isEqualTo(it.loadBias + 0x100UL)
            assertThat(it.executableEnd).isEqualTo(it.loadBias + 0x180UL)
            assertThat(it.buildId).isNull()
        }
    }

    @Test
    fun overflowingRangesAreSkippedAndAllExecutableSegmentsAreKept() {
        val snapshot = parse(1)
        val maximum = if (snapshot.architecture.pointerSize == 4) UInt.MAX_VALUE.toULong() else ULong.MAX_VALUE
        assertThat(snapshot.modules.map { it.name }).containsExactly("libhighest.so", "libmultiple.so", "libmultiple.so")
        assertThat(snapshot.modules.first().executableEnd).isEqualTo(maximum)
        assertThat(snapshot.modules.drop(1).map { it.executableStart }).containsExactly(0x100UL, 0x400UL)
    }

    @Test
    fun namesMustFitAndContainValidNonblankUtf8WithoutControls() {
        assertThat(modules(2).map { it.name }).containsExactly("lib\u00e9.so", "a".repeat(63), "lib\uDBFF\uDFFF.so")
    }

    @Test
    fun appLibrariesDisplaceTheLastSystemEntriesWhenFull() {
        val modules = modules(3)
        assertThat(modules).hasSize(NativeCrashSnapshotLayout.MAX_MODULES)
        assertThat(modules.take(2).map { it.name }).containsExactly("libfirst.so", "libsecond.so")
        assertThat(modules.drop(2).map { it.loadBias }).containsExactlyElementsOf((0 until 126).map { it.toULong() * 0x1000UL })
    }

    @Test
    fun fullAppTableKeepsTheFirstAppSegmentsWithoutOverflow() {
        val modules = modules(4)
        assertThat(modules.map { it.loadBias }).containsExactlyElementsOf((0 until 128).map { it.toULong() * 0x1000UL })
    }

    @Test
    fun onlyTheOriginalLoaderInfoFieldsAreRequired() {
        assertThat(modules(5).map { it.name }).containsExactly("libold.so")
    }

    @Test
    fun enumerationFailureAndEmptyInputClearTheWholeTable() {
        for (scenario in listOf(6, 7, 9)) {
            val bytes = NativeCrashModuleTestJni.snapshot(scenario)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            assertThat(buffer.getInt(NativeCrashSnapshotLayout.MODULE_COUNT_OFFSET)).isZero()
            assertThat(bytes.copyOfRange(NativeCrashSnapshotLayout.MODULES_OFFSET, NativeCrashSnapshotLayout.STACK_OFFSET))
                .containsOnly(0)
            assertThat(NativeCrashSnapshotParser.parse(bytes, crash)).isNull()
        }
    }

    @Test
    fun realLoaderTableResolvesThisNativeFunctionToItsModuleRelativeFrame() {
        val snapshot = parse(8)
        val rawAddress = NativeCrashModuleTestJni.functionAddress().toULong()
        val address = if (snapshot.architecture == NativeCrashArchitecture.ARM) rawAddress and 1UL.inv() else rawAddress
        val module = requireNotNull(snapshot.modules.find { address >= it.executableStart && address < it.executableEnd })
        // API 23 can report the APK container rather than its embedded library path.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            assertThat(module.name).isIn("libotel_android_native_crash.so", "base.apk")
        } else {
            assertThat(module.name).isEqualTo("libotel_android_native_crash.so")
        }
        assertThat(NativeCrashSnapshotUnwinder.unwind(snapshot)).containsExactly(
            NativeCrashFrame(module.name, address - module.loadBias, null, NativeCrashFrameOrigin.PROGRAM_COUNTER),
        )
    }

    private fun modules(scenario: Int): List<NativeCrashModule> = parse(scenario).modules

    private fun parse(scenario: Int): NativeCrashSnapshot {
        val bytes = NativeCrashModuleTestJni.snapshot(scenario)
        val snapshot = requireNotNull(NativeCrashSnapshotParser.parse(bytes, crash))
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        // A parser that silently drops a malformed native entry must not make this test pass.
        assertThat(snapshot.modules).hasSize(buffer.getInt(NativeCrashSnapshotLayout.MODULE_COUNT_OFFSET))
        val unusedStart = NativeCrashSnapshotLayout.MODULES_OFFSET + snapshot.modules.size * NativeCrashSnapshotLayout.MODULE_ENTRY_SIZE
        val unused = bytes.copyOfRange(unusedStart, NativeCrashSnapshotLayout.STACK_OFFSET)
        assertThat(unused).isEqualTo(ByteArray(unused.size))
        return snapshot
    }
}

internal object NativeCrashModuleTestJni {
    @JvmStatic external fun snapshot(scenario: Int): ByteArray

    @JvmStatic external fun functionAddress(): Long
}
