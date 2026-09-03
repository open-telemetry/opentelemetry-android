/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class NativeCrashSnapshotUnwinderTest {
    @TestFactory
    fun `resolves code addresses`() =
        listOf(
            case("program counter and build id", snapshot(), frame(0x120UL, PROGRAM_COUNTER)),
            case("ARM Thumb bit", snapshot(ARM, programCounter = 0x1123UL), frame(0x122UL, PROGRAM_COUNTER)),
            case("x86 keeps ARM64 tag bits", snapshot(X86, programCounter = TAGGED_PC)),
            case("x86_64 keeps ARM64 tag bits", snapshot(X86_64, programCounter = TAGGED_PC)),
            case("executable start", snapshot(programCounter = 0x1100UL), frame(0x100UL, PROGRAM_COUNTER)),
            case("executable end", snapshot(programCounter = 0x2000UL)),
            case(
                "load bias underflow",
                snapshot(modules = listOf(module(loadBias = 0x1200UL, start = 0x1100UL))),
            ),
            case(
                "raw ARM64 address wins",
                snapshot(
                    ARM64,
                    programCounter = TAGGED_PC,
                    modules =
                        listOf(
                            module(),
                            module("libtagged.so", TAGGED_PC - 0x120UL, TAGGED_PC - 0x20UL, TAGGED_PC + 0x100UL),
                        ),
                ),
                frame(0x120UL, PROGRAM_COUNTER, "libtagged.so"),
            ),
        ) + arm64AddressCases().map { (name, address, normalized) -> normalizedArm64Case(name, address, normalized) }

    @TestFactory
    fun `adjusts caller return addresses`() =
        listOf(
            case("x86", oneRecordSnapshot(X86, programCounter = 0UL), frame(0x127UL, FRAME_POINTER)),
            case("x86_64", oneRecordSnapshot(returnAddress = 0x2000UL, programCounter = 0UL), frame(0xfffUL, FRAME_POINTER)),
            case("ARM64", snapshot(ARM64, programCounter = 0UL, linkRegister = 0x2000UL), frame(0xffcUL, LINK_REGISTER)),
            case("ARM Thumb", snapshot(ARM, programCounter = 0UL, linkRegister = 0x2001UL), frame(0xffeUL, LINK_REGISTER)),
            case("ARM state", snapshot(ARM, programCounter = 0UL, linkRegister = 0x2000UL), frame(0xffcUL, LINK_REGISTER)),
        )

    @Test
    fun `walks frame records and keeps module build ids`() {
        val feature = module("libfeature.so", 0x3000UL, 0x3100UL, 0x4000UL, null)
        val stack = stack(48, 8, record(STACK_START, STACK_START + 16UL, 0x1128UL), record(STACK_START + 16UL, 0UL, 0x3120UL))

        assertUnwinds(
            snapshot(ARM64, framePointer = STACK_START, modules = listOf(module(), feature), stack = stack),
            frame(0x120UL, PROGRAM_COUNTER),
            frame(0x124UL, FRAME_POINTER),
            frame(0x11cUL, FRAME_POINTER, "libfeature.so", null),
        )
    }

    @TestFactory
    fun `normalizes only top-byte tags on ARM64 stack addresses`() =
        listOf(
            case(
                "tagged frame pointers",
                snapshot(
                    ARM64,
                    framePointer = TAGGED_STACK_START,
                    stackStart = TAGGED_STACK_START,
                    stack = stack(48, 8, record(STACK_START, 0x4b00_0000_0000_7010UL, 0x1128UL), record(STACK_START + 16UL, 0UL, 0x1130UL)),
                ),
                frame(0x120UL, PROGRAM_COUNTER),
                frame(0x124UL, FRAME_POINTER),
                frame(0x12cUL, FRAME_POINTER),
            ),
            case(
                "unrelated virtual-address bits",
                snapshot(
                    ARM64,
                    programCounter = 0UL,
                    framePointer = 0x0000_1200_0000_7008UL,
                    stackStart = 0x0000_7f00_0000_7000UL,
                    stack =
                        ByteArray(24).also { bytes ->
                            bytes[16] = 0x28
                            bytes[17] = 0x11
                        },
                ),
            ),
        )

    @TestFactory
    fun `uses link registers conservatively`() =
        listOf(
            case(
                "ARM does not walk frame records",
                oneRecordSnapshot(ARM, 0x1180UL, 0x1121UL, 0x1131UL),
                frame(0x120UL, PROGRAM_COUNTER),
                frame(0x12eUL, LINK_REGISTER),
            ),
            case(
                "resolved caller suppresses ARM64 link register",
                oneRecordSnapshot(ARM64, linkRegister = 0x1130UL),
                frame(0x120UL, PROGRAM_COUNTER),
                frame(0x124UL, FRAME_POINTER),
            ),
            case(
                "unresolved caller uses ARM64 link register",
                oneRecordSnapshot(ARM64, 0x9000UL, linkRegister = 0x1130UL),
                frame(0x120UL, PROGRAM_COUNTER),
                frame(0x12cUL, LINK_REGISTER),
            ),
            case(
                "missing PC puts link register before older callers",
                oneRecordSnapshot(ARM64, 0x1180UL, 0UL, 0x1130UL),
                frame(0x12cUL, LINK_REGISTER),
                frame(0x17cUL, FRAME_POINTER),
            ),
            case(
                "frame record de-duplicates link register",
                oneRecordSnapshot(ARM64, 0x1130UL, 0UL, 0x1130UL),
                frame(0x12cUL, FRAME_POINTER),
            ),
            case("x86 ignores link register", snapshot(X86_64, programCounter = 0UL, linkRegister = 0x1130UL)),
            case("PC de-duplicates link register", snapshot(ARM64, linkRegister = 0x1124UL), frame(0x120UL, PROGRAM_COUNTER)),
        )

    @TestFactory
    fun `keeps same-offset frames with distinct module identities`() =
        listOf(
            "module name" to ("libfeature.so" to "0123abcd"),
            "build id" to ("libapp.so" to "5678efab"),
        ).map { (name, identity) ->
            val second = module(identity.first, 0x3000UL, 0x3100UL, 0x4000UL, identity.second)
            case(
                name,
                snapshot(ARM64, linkRegister = 0x3124UL, modules = listOf(module(), second)),
                frame(0x120UL, PROGRAM_COUNTER),
                frame(0x120UL, LINK_REGISTER, identity.first, identity.second),
            )
        }

    @TestFactory
    fun `stops safely on invalid frame records`() =
        listOf(
            "zero frame pointer" to snapshot(framePointer = 0UL, stack = ByteArray(16)),
            "below stack" to snapshot(framePointer = STACK_START - 8UL, stack = ByteArray(16)),
            "past stack" to snapshot(framePointer = STACK_START + 24UL, stack = ByteArray(16)),
            "truncated record" to snapshot(framePointer = STACK_START, stack = ByteArray(8)),
            "partial record at stack end" to snapshot(framePointer = STACK_START + 16UL, stack = ByteArray(24)),
            "misaligned record" to
                snapshot(
                    framePointer = STACK_START + 1UL,
                    stack = stack(24, 8, record(STACK_START + 1UL, 0UL, 0x1128UL)),
                ),
            "overflowing return slot" to
                snapshot(framePointer = ULong.MAX_VALUE - 7UL, stackStart = ULong.MAX_VALUE - 7UL, stack = ByteArray(16)),
        ).map { (name, input) -> case(name, input, frame(0x120UL, PROGRAM_COUNTER)) }

    @TestFactory
    fun `stops safely on invalid frame chains`() =
        listOf(
            case(
                "same frame pointer",
                oneRecordSnapshot(previousFramePointer = STACK_START),
                frame(0x120UL, PROGRAM_COUNTER),
                frame(0x127UL, FRAME_POINTER),
            ),
            case(
                "decreasing frame pointer",
                snapshot(
                    framePointer = STACK_START + 16UL,
                    stack =
                        stack(
                            32,
                            8,
                            record(STACK_START, 0UL, 0x1130UL),
                            record(STACK_START + 16UL, STACK_START, 0x1128UL),
                        ),
                ),
                frame(0x120UL, PROGRAM_COUNTER),
                frame(0x127UL, FRAME_POINTER),
            ),
            case(
                "unresolved record",
                snapshot(
                    framePointer = STACK_START,
                    stack =
                        stack(
                            48,
                            8,
                            record(STACK_START, STACK_START + 16UL, 0x9000UL),
                            record(STACK_START + 16UL, 0UL, 0x1128UL),
                        ),
                ),
                frame(0x120UL, PROGRAM_COUNTER),
                frame(0x127UL, FRAME_POINTER),
            ),
        )

    @Test
    fun `caps recovered output at 64 frames`() {
        val frames = unwindLongChain(unresolvedRecords = 0)
        assertThat(frames).hasSize(64)
        assertThat(frames.count { it.origin == PROGRAM_COUNTER }).isEqualTo(1)
        assertThat(frames.count { it.origin == FRAME_POINTER }).isEqualTo(63)
    }

    @Test
    fun `unresolved records do not consume the frame limit`() {
        assertThat(unwindLongChain(unresolvedRecords = 64))
            .hasSize(64)
            .allMatch { it == frame(0x127UL, FRAME_POINTER) }
    }

    private fun unwindLongChain(unresolvedRecords: Int): List<NativeCrashFrame> {
        val count = unresolvedRecords + 70
        val stack = ByteArray(count * 2 * Long.SIZE_BYTES)
        repeat(count) { index ->
            val address = STACK_START + index.toULong() * 16UL
            val previous = if (index == count - 1) 0UL else address + 16UL
            val returnAddress = if (index < unresolvedRecords) 0x9000UL else 0x1128UL
            stack.writeFrame(address, previous, returnAddress, Long.SIZE_BYTES)
        }
        val programCounter = if (unresolvedRecords == 0) 0x1120UL else 0UL
        return NativeCrashSnapshotUnwinder.unwind(
            snapshot(programCounter = programCounter, framePointer = STACK_START, stack = stack),
        )
    }

    private fun normalizedArm64Case(
        name: String,
        address: ULong,
        normalized: ULong,
    ): DynamicTest {
        val target = module(loadBias = normalized - 0x120UL, start = normalized - 0x20UL, end = normalized + 0x100UL)
        return case(name, snapshot(ARM64, programCounter = address, modules = listOf(target)), frame(0x120UL, PROGRAM_COUNTER))
    }

    private fun oneRecordSnapshot(
        architecture: NativeCrashArchitecture = X86_64,
        returnAddress: ULong = 0x1128UL,
        programCounter: ULong = 0x1120UL,
        linkRegister: ULong = 0UL,
        previousFramePointer: ULong = 0UL,
    ): NativeCrashSnapshot =
        snapshot(
            architecture,
            programCounter,
            STACK_START,
            linkRegister,
            stack = stack(architecture.pointerSize * 2, architecture.pointerSize, record(STACK_START, previousFramePointer, returnAddress)),
        )

    private fun case(
        name: String,
        snapshot: NativeCrashSnapshot,
        vararg frames: NativeCrashFrame,
    ): DynamicTest = dynamicTest(name) { assertUnwinds(snapshot, *frames) }

    private fun assertUnwinds(
        snapshot: NativeCrashSnapshot,
        vararg frames: NativeCrashFrame,
    ) {
        assertThat(NativeCrashSnapshotUnwinder.unwind(snapshot)).containsExactly(*frames)
    }

    private fun snapshot(
        architecture: NativeCrashArchitecture = X86_64,
        programCounter: ULong = 0x1120UL,
        framePointer: ULong = 0UL,
        linkRegister: ULong = 0UL,
        modules: List<NativeCrashModule> = listOf(module()),
        stackStart: ULong = STACK_START,
        stack: ByteArray = byteArrayOf(),
    ) = NativeCrashSnapshot(architecture, programCounter, stackStart, framePointer, linkRegister, modules, stackStart, stack)

    private fun module(
        name: String = "libapp.so",
        loadBias: ULong = 0x1000UL,
        start: ULong = 0x1100UL,
        end: ULong = 0x2000UL,
        buildId: String? = "0123abcd",
    ) = NativeCrashModule(loadBias, start, end, name, buildId)

    private fun frame(
        address: ULong,
        origin: NativeCrashFrameOrigin,
        name: String = "libapp.so",
        buildId: String? = "0123abcd",
    ) = NativeCrashFrame(name, address, buildId, origin)

    private fun stack(
        size: Int,
        pointerSize: Int,
        vararg records: Record,
    ) = ByteArray(size).apply { records.forEach { writeFrame(it.address, it.previous, it.returnAddress, pointerSize) } }

    private fun record(
        address: ULong,
        previous: ULong,
        returnAddress: ULong,
    ) = Record(address, previous, returnAddress)

    private fun ByteArray.writeFrame(
        address: ULong,
        previous: ULong,
        returnAddress: ULong,
        pointerSize: Int,
    ) {
        writePointer(address, previous, pointerSize)
        writePointer(address + pointerSize.toULong(), returnAddress, pointerSize)
    }

    private fun ByteArray.writePointer(
        address: ULong,
        value: ULong,
        pointerSize: Int,
    ) {
        val offset = (address - STACK_START).toInt()
        repeat(pointerSize) { index -> this[offset + index] = (value shr (index * Byte.SIZE_BITS)).toByte() }
    }

    private fun arm64AddressCases() =
        listOf(
            Triple("ARM64 clears bits 56-63", 0xaba0_0000_0000_1120UL, 0x00a0_0000_0000_1120UL),
            Triple("ARM64 clears bits 52-63", 0x00fa_0000_0000_1120UL, 0x000a_0000_0000_1120UL),
            Triple("ARM64 clears bits 48-63", 0x000a_a000_0000_1120UL, 0x0000_a000_0000_1120UL),
            Triple("ARM64 clears bits 47-63", 0x0000_c000_0000_1120UL, 0x0000_4000_0000_1120UL),
            Triple("ARM64 clears bits 39-63", 0x0000_00c0_0000_1120UL, 0x0000_0040_0000_1120UL),
        )

    private data class Record(
        val address: ULong,
        val previous: ULong,
        val returnAddress: ULong,
    )

    private companion object {
        const val STACK_START = 0x7000UL
        const val TAGGED_STACK_START = 0xab00_0000_0000_7000UL
        const val TAGGED_PC = 0xab00_0000_0000_1120UL
        val ARM = NativeCrashArchitecture.ARM
        val ARM64 = NativeCrashArchitecture.ARM64
        val X86 = NativeCrashArchitecture.X86
        val X86_64 = NativeCrashArchitecture.X86_64
        val PROGRAM_COUNTER = NativeCrashFrameOrigin.PROGRAM_COUNTER
        val FRAME_POINTER = NativeCrashFrameOrigin.FRAME_POINTER
        val LINK_REGISTER = NativeCrashFrameOrigin.LINK_REGISTER
    }
}
