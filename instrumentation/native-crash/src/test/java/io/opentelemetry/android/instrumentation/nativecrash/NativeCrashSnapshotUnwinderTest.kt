/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class NativeCrashSnapshotUnwinderTest {
    @Test
    fun `recovers a module-relative program counter and build id`() {
        assertThat(NativeCrashSnapshotUnwinder.unwind(snapshot()))
            .containsExactly(
                NativeCrashFrame(
                    moduleName = "libapp.so",
                    moduleRelativeAddress = 0x120UL,
                    buildId = "0123abcd",
                    origin = NativeCrashFrameOrigin.PROGRAM_COUNTER,
                ),
            )
    }

    @Test
    fun `clears the ARM thumb bit`() {
        val frames =
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(
                    architecture = NativeCrashArchitecture.ARM,
                    programCounter = 0x1123UL,
                ),
            )

        assertThat(frames.single().moduleRelativeAddress).isEqualTo(0x122UL)
    }

    @TestFactory
    fun `normalizes ARM64 addresses in contract order`() =
        listOf(
            Arm64AddressCase(
                name = "bits 56 through 63",
                address = 0xaba0_0000_0000_1120UL,
                normalizedAddress = 0x00a0_0000_0000_1120UL,
            ),
            Arm64AddressCase(
                name = "bits 52 through 63",
                address = 0x00fa_0000_0000_1120UL,
                normalizedAddress = 0x000a_0000_0000_1120UL,
            ),
            Arm64AddressCase(
                name = "bits 48 through 63",
                address = 0x000a_a000_0000_1120UL,
                normalizedAddress = 0x0000_a000_0000_1120UL,
            ),
            Arm64AddressCase(
                name = "bits 47 through 63",
                address = 0x0000_c000_0000_1120UL,
                normalizedAddress = 0x0000_4000_0000_1120UL,
            ),
            Arm64AddressCase(
                name = "bits 39 through 63",
                address = 0x0000_00c0_0000_1120UL,
                normalizedAddress = 0x0000_0040_0000_1120UL,
            ),
        ).map { case ->
            dynamicTest(case.name) {
                val normalizedModule =
                    module(
                        loadBias = case.normalizedAddress - 0x120UL,
                        start = case.normalizedAddress - 0x20UL,
                        end = case.normalizedAddress + 0x100UL,
                    )
                val frame =
                    NativeCrashSnapshotUnwinder
                        .unwind(
                            snapshot(
                                architecture = NativeCrashArchitecture.ARM64,
                                programCounter = case.address,
                                modules = listOf(normalizedModule),
                            ),
                        ).single()

                assertThat(frame.moduleRelativeAddress).isEqualTo(0x120UL)
            }
        }

    @TestFactory
    fun `does not apply ARM address normalization to x86`() =
        listOf(NativeCrashArchitecture.X86, NativeCrashArchitecture.X86_64).map { architecture ->
            dynamicTest(architecture.name) {
                assertThat(
                    NativeCrashSnapshotUnwinder.unwind(
                        snapshot(
                            architecture = architecture,
                            programCounter = 0xab00_0000_0000_1120UL,
                        ),
                    ),
                ).isEmpty()
            }
        }

    @Test
    fun `prefers a raw ARM64 address before stripped candidates`() {
        val rawAddress = 0xab00_0000_0000_1120UL
        val rawModule =
            module(
                name = "libtagged.so",
                loadBias = rawAddress - 0x120UL,
                start = rawAddress - 0x20UL,
                end = rawAddress + 0x100UL,
            )
        val frame =
            NativeCrashSnapshotUnwinder
                .unwind(
                    snapshot(
                        architecture = NativeCrashArchitecture.ARM64,
                        programCounter = rawAddress,
                        modules = listOf(module(), rawModule),
                    ),
                ).single()

        assertThat(frame.moduleName).isEqualTo("libtagged.so")
        assertThat(frame.moduleRelativeAddress).isEqualTo(0x120UL)
    }

    @Test
    fun `uses executable start and excludes executable end`() {
        assertThat(
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(programCounter = 0x1100UL),
            ),
        ).containsExactly(frame(0x100UL, NativeCrashFrameOrigin.PROGRAM_COUNTER))
        assertThat(
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(programCounter = 0x2000UL),
            ),
        ).isEmpty()
    }

    @Test
    fun `does not underflow a module-relative address`() {
        assertThat(
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(
                    modules =
                        listOf(
                            module(
                                loadBias = 0x1200UL,
                                start = 0x1100UL,
                            ),
                        ),
                ),
            ),
        ).isEmpty()
    }

    @Test
    fun `walks ARM64 frame records and keeps each module build id`() {
        val stack = ByteArray(48)
        stack.writeFrame(
            framePointer = STACK_START,
            previousFramePointer = STACK_START + 16UL,
            returnAddress = 0x1128UL,
            pointerSize = Long.SIZE_BYTES,
        )
        stack.writeFrame(
            framePointer = STACK_START + 16UL,
            previousFramePointer = 0UL,
            returnAddress = 0x3120UL,
            pointerSize = Long.SIZE_BYTES,
        )
        val frames =
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(
                    architecture = NativeCrashArchitecture.ARM64,
                    framePointer = STACK_START,
                    linkRegister = 0x1130UL,
                    modules =
                        listOf(
                            module(),
                            module(
                                name = "libfeature.so",
                                loadBias = 0x3000UL,
                                start = 0x3100UL,
                                end = 0x4000UL,
                                buildId = null,
                            ),
                        ),
                    stack = stack,
                ),
            )

        assertThat(frames)
            .containsExactly(
                frame(0x120UL, NativeCrashFrameOrigin.PROGRAM_COUNTER),
                frame(0x128UL, NativeCrashFrameOrigin.FRAME_POINTER),
                NativeCrashFrame("libfeature.so", 0x120UL, null, NativeCrashFrameOrigin.FRAME_POINTER),
            )
    }

    @Test
    fun `walks little endian 32 bit frame records`() {
        val stack = ByteArray(8)
        stack.writeFrame(
            framePointer = STACK_START,
            previousFramePointer = 0UL,
            returnAddress = 0x1128UL,
            pointerSize = Int.SIZE_BYTES,
        )

        assertThat(
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(
                    architecture = NativeCrashArchitecture.X86,
                    programCounter = 0UL,
                    framePointer = STACK_START,
                    stack = stack,
                ),
            ),
        ).containsExactly(frame(0x128UL, NativeCrashFrameOrigin.FRAME_POINTER))
    }

    @Test
    fun `ARM uses the link register without walking frame records`() {
        val stack = ByteArray(8)
        stack.writeFrame(STACK_START, 0UL, 0x1180UL, Int.SIZE_BYTES)

        assertThat(
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(
                    architecture = NativeCrashArchitecture.ARM,
                    programCounter = 0x1121UL,
                    framePointer = STACK_START,
                    linkRegister = 0x1131UL,
                    stack = stack,
                ),
            ),
        ).containsExactly(
            frame(0x120UL, NativeCrashFrameOrigin.PROGRAM_COUNTER),
            frame(0x130UL, NativeCrashFrameOrigin.LINK_REGISTER),
        )
    }

    @Test
    fun `ARM64 uses the link register only when no caller resolves`() {
        val resolvedStack = ByteArray(16)
        resolvedStack.writeFrame(STACK_START, 0UL, 0x1128UL, Long.SIZE_BYTES)
        val unresolvedStack = ByteArray(16)
        unresolvedStack.writeFrame(STACK_START, 0UL, 0x9000UL, Long.SIZE_BYTES)

        assertThat(
            NativeCrashSnapshotUnwinder
                .unwind(
                    snapshot(
                        architecture = NativeCrashArchitecture.ARM64,
                        framePointer = STACK_START,
                        linkRegister = 0x1130UL,
                        stack = resolvedStack,
                    ),
                ).map { it.origin },
        ).containsExactly(NativeCrashFrameOrigin.PROGRAM_COUNTER, NativeCrashFrameOrigin.FRAME_POINTER)
        assertThat(
            NativeCrashSnapshotUnwinder
                .unwind(
                    snapshot(
                        architecture = NativeCrashArchitecture.ARM64,
                        framePointer = STACK_START,
                        linkRegister = 0x1130UL,
                        stack = unresolvedStack,
                    ),
                ).map { it.origin },
        ).containsExactly(NativeCrashFrameOrigin.PROGRAM_COUNTER, NativeCrashFrameOrigin.LINK_REGISTER)
    }

    @Test
    fun `continues walking after an unresolved return address`() {
        val stack = ByteArray(48)
        stack.writeFrame(STACK_START, STACK_START + 16UL, 0x9000UL, Long.SIZE_BYTES)
        stack.writeFrame(STACK_START + 16UL, 0UL, 0x1128UL, Long.SIZE_BYTES)

        assertThat(
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(framePointer = STACK_START, stack = stack),
            ),
        ).containsExactly(
            frame(0x120UL, NativeCrashFrameOrigin.PROGRAM_COUNTER),
            frame(0x128UL, NativeCrashFrameOrigin.FRAME_POINTER),
        )
    }

    @TestFactory
    fun `stops safely when a frame record cannot be read`() =
        listOf(
            "zero frame pointer" to snapshot(framePointer = 0UL, stack = ByteArray(16)),
            "below captured stack" to snapshot(framePointer = STACK_START - 8UL, stack = ByteArray(16)),
            "past captured stack" to snapshot(framePointer = STACK_START + 24UL, stack = ByteArray(16)),
            "truncated record" to snapshot(framePointer = STACK_START, stack = ByteArray(8)),
            "overflowing return slot" to
                snapshot(
                    framePointer = ULong.MAX_VALUE - 7UL,
                    stackStart = ULong.MAX_VALUE - 7UL,
                    stack = ByteArray(16),
                ),
        ).map { (name, snapshot) ->
            dynamicTest(name) {
                assertThat(NativeCrashSnapshotUnwinder.unwind(snapshot))
                    .containsExactly(frame(0x120UL, NativeCrashFrameOrigin.PROGRAM_COUNTER))
            }
        }

    @Test
    fun `rejects a readable but misaligned frame record`() {
        val stack = ByteArray(24)
        stack.writeFrame(STACK_START + 1UL, 0UL, 0x1128UL, Long.SIZE_BYTES)

        assertThat(
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(framePointer = STACK_START + 1UL, stack = stack),
            ),
        ).containsExactly(frame(0x120UL, NativeCrashFrameOrigin.PROGRAM_COUNTER))
    }

    @Test
    fun `stops a non-increasing frame chain after the current caller`() {
        val stack = ByteArray(16)
        stack.writeFrame(STACK_START, STACK_START, 0x1128UL, Long.SIZE_BYTES)

        assertThat(
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(framePointer = STACK_START, stack = stack),
            ),
        ).containsExactly(
            frame(0x120UL, NativeCrashFrameOrigin.PROGRAM_COUNTER),
            frame(0x128UL, NativeCrashFrameOrigin.FRAME_POINTER),
        )
    }

    @Test
    fun `stops a decreasing frame chain after the current caller`() {
        val stack = ByteArray(32)
        stack.writeFrame(STACK_START, 0UL, 0x1130UL, Long.SIZE_BYTES)
        stack.writeFrame(STACK_START + 16UL, STACK_START, 0x1128UL, Long.SIZE_BYTES)

        assertThat(
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(framePointer = STACK_START + 16UL, stack = stack),
            ),
        ).containsExactly(
            frame(0x120UL, NativeCrashFrameOrigin.PROGRAM_COUNTER),
            frame(0x128UL, NativeCrashFrameOrigin.FRAME_POINTER),
        )
    }

    @Test
    fun `caps recovered output at 64 frames`() {
        val stack = ByteArray(70 * 2 * Long.SIZE_BYTES)
        repeat(70) { index ->
            val framePointer = STACK_START + index.toULong() * 16UL
            val previous = if (index == 69) 0UL else framePointer + 16UL
            stack.writeFrame(framePointer, previous, 0x1128UL, Long.SIZE_BYTES)
        }

        val frames =
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(framePointer = STACK_START, stack = stack),
            )

        assertThat(frames).hasSize(64)
        assertThat(frames.count { it.origin == NativeCrashFrameOrigin.PROGRAM_COUNTER }).isEqualTo(1)
        assertThat(frames.count { it.origin == NativeCrashFrameOrigin.FRAME_POINTER }).isEqualTo(63)
    }

    @Test
    fun `continues bounded traversal after 64 unresolved records`() {
        val stack = ByteArray(66 * 2 * Long.SIZE_BYTES)
        repeat(66) { index ->
            val framePointer = STACK_START + index.toULong() * 16UL
            val previous = if (index == 65) 0UL else framePointer + 16UL
            val returnAddress = if (index == 64) 0x1128UL else 0x9000UL
            stack.writeFrame(framePointer, previous, returnAddress, Long.SIZE_BYTES)
        }

        assertThat(
            NativeCrashSnapshotUnwinder.unwind(
                snapshot(
                    programCounter = 0UL,
                    framePointer = STACK_START,
                    stack = stack,
                ),
            ),
        ).containsExactly(frame(0x128UL, NativeCrashFrameOrigin.FRAME_POINTER))
    }

    private fun snapshot(
        architecture: NativeCrashArchitecture = NativeCrashArchitecture.X86_64,
        programCounter: ULong = 0x1120UL,
        framePointer: ULong = 0UL,
        linkRegister: ULong = 0UL,
        modules: List<NativeCrashModule> = listOf(module()),
        stackStart: ULong = STACK_START,
        stack: ByteArray = byteArrayOf(),
    ): NativeCrashSnapshot =
        NativeCrashSnapshot(
            architecture = architecture,
            programCounter = programCounter,
            stackPointer = stackStart,
            framePointer = framePointer,
            linkRegister = linkRegister,
            modules = modules,
            stackStart = stackStart,
            stack = stack,
        )

    private fun module(
        name: String = "libapp.so",
        loadBias: ULong = 0x1000UL,
        start: ULong = 0x1100UL,
        end: ULong = 0x2000UL,
        buildId: String? = "0123abcd",
    ): NativeCrashModule = NativeCrashModule(loadBias, start, end, name, buildId)

    private fun frame(
        relativeAddress: ULong,
        origin: NativeCrashFrameOrigin,
    ): NativeCrashFrame = NativeCrashFrame("libapp.so", relativeAddress, "0123abcd", origin)

    private fun ByteArray.writeFrame(
        framePointer: ULong,
        previousFramePointer: ULong,
        returnAddress: ULong,
        pointerSize: Int,
    ) {
        writePointer(framePointer, previousFramePointer, pointerSize)
        writePointer(framePointer + pointerSize.toULong(), returnAddress, pointerSize)
    }

    private fun ByteArray.writePointer(
        address: ULong,
        value: ULong,
        pointerSize: Int,
    ) {
        val offset = (address - STACK_START).toInt()
        repeat(pointerSize) { index ->
            this[offset + index] = ((value shr (index * Byte.SIZE_BITS)) and 0xffUL).toByte()
        }
    }

    private companion object {
        const val STACK_START = 0x7000UL
    }

    private data class Arm64AddressCase(
        val name: String,
        val address: ULong,
        val normalizedAddress: ULong,
    )
}
