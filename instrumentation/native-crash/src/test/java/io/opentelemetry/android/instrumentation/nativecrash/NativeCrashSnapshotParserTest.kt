/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import kotlin.random.Random

class NativeCrashSnapshotParserTest {
    @Test
    fun `uses the contract checksum constants`() {
        val bytes = ByteArray(NativeCrashSnapshotLayout.RECORD_SIZE)
        bytes[0] = 1
        bytes[1] = 2
        bytes[2] = 3
        bytes[NativeCrashSnapshotLayout.CHECKSUM_OFFSET - 1] = 0xff.toByte()

        assertThat(NativeCrashSnapshotParser.checksum(bytes)).isEqualTo(0x974ee9fcu)
    }

    @Test
    fun `checksum rejects a truncated prefix`() {
        assertThatThrownBy {
            NativeCrashSnapshotParser.checksum(ByteArray(NativeCrashSnapshotLayout.CHECKSUM_OFFSET - 1))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `parses a complete snapshot at maximum bounds`() {
        val snapshot =
            requireNotNull(
                parse { buffer ->
                    buffer.writeMaximumModuleTable()
                    buffer.putInt(NativeCrashSnapshotLayout.STACK_SIZE_OFFSET, 4096)
                    buffer.fill(MODULE_OFFSET + 24, 64, 0)
                    buffer.fill(MODULE_OFFSET + 24, 63, 'a'.code.toByte())
                    buffer.putInt(MODULE_OFFSET + 88, 32)
                    buffer.position(MODULE_OFFSET + 92)
                    buffer.put(ByteArray(32) { it.toByte() })
                },
            )

        assertThat(snapshot.architecture).isEqualTo(NativeCrashArchitecture.ARM64)
        assertThat(snapshot.programCounter).isEqualTo(0x1120UL)
        assertThat(snapshot.stackPointer).isEqualTo(STACK_START.toULong())
        assertThat(snapshot.framePointer).isEqualTo((STACK_START + 16).toULong())
        assertThat(snapshot.linkRegister).isEqualTo(0x1130UL)
        assertThat(snapshot.stack).hasSize(4096)
        assertThat(snapshot.stack.copyOf(4)).containsExactly(1, 2, 3, 4)
        assertThat(snapshot.modules).hasSize(NativeCrashSnapshotLayout.MAX_MODULES)
        assertThat(snapshot.modules.first())
            .isEqualTo(
                NativeCrashModule(
                    0x1000UL,
                    0x1100UL,
                    0x2000UL,
                    "a".repeat(63),
                    "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f",
                ),
            )
        assertThat(snapshot.modules.last())
            .isEqualTo(NativeCrashModule(0xaef0UL, 0xaf00UL, 0xaf80UL, "lib127.so", null))
    }

    @TestFactory
    fun `supports each contract architecture`() =
        NativeCrashArchitecture.entries.map { architecture ->
            dynamicTest(architecture.name) {
                val encodedLinkRegister =
                    if (architecture == NativeCrashArchitecture.ARM) {
                        UInt.MAX_VALUE.toLong()
                    } else {
                        -1L
                    }
                val snapshot =
                    requireNotNull(
                        parse(architecture) { buffer ->
                            buffer.putLong(NativeCrashSnapshotLayout.LINK_REGISTER_OFFSET, encodedLinkRegister)
                        },
                    )

                assertThat(snapshot.architecture).isEqualTo(architecture)
                assertThat(snapshot.linkRegister)
                    .isEqualTo(if (architecture.hasLinkRegister) encodedLinkRegister.toULong() else 0UL)
                assertThat(snapshot.stack).containsExactly(1, 2, 3, 4)
            }
        }

    @Test
    fun `accepts an empty stack and zero program counter`() {
        val snapshot =
            requireNotNull(
                parse { buffer ->
                    buffer.putLong(NativeCrashSnapshotLayout.PROGRAM_COUNTER_OFFSET, 0)
                    buffer.putInt(NativeCrashSnapshotLayout.STACK_SIZE_OFFSET, 0)
                },
            )

        assertThat(snapshot.programCounter).isEqualTo(0UL)
        assertThat(snapshot.stack).isEmpty()
    }

    @Test
    fun `parsed build id length is preserved during unwind`() {
        assertThat(NativeCrashSnapshotUnwinder.unwind(requireNotNull(parse())))
            .containsExactly(
                NativeCrashFrame(
                    moduleName = "libapp.so",
                    moduleRelativeAddress = 0x120UL,
                    buildId = "0123fe",
                    origin = NativeCrashFrameOrigin.PROGRAM_COUNTER,
                ),
                NativeCrashFrame(
                    moduleName = "libapp.so",
                    moduleRelativeAddress = 0x12cUL,
                    buildId = "0123fe",
                    origin = NativeCrashFrameOrigin.LINK_REGISTER,
                ),
            )
    }

    @Test
    fun `rejects size checksum signal and timestamp mismatches`() {
        val valid = SnapshotBuilder().build()
        val corrupt = valid.copyOf().apply { this[NativeCrashSnapshotLayout.STACK_OFFSET] = 9 }

        assertThat(NativeCrashSnapshotParser.parse(valid.copyOf(valid.size - 1), crashRecord)).isNull()
        assertThat(NativeCrashSnapshotParser.parse(valid + byteArrayOf(0), crashRecord)).isNull()
        assertThat(NativeCrashSnapshotParser.parse(corrupt, crashRecord)).isNull()
        assertThat(NativeCrashSnapshotParser.parse(valid, crashRecord.copy(signalNumber = 6))).isNull()
        assertThat(NativeCrashSnapshotParser.parse(valid, crashRecord.copy(timestamp = crashRecord.timestamp.plusNanos(1)))).isNull()
        assertThat(NativeCrashSnapshotParser.parse(valid, crashRecord.copy(timestamp = Instant.MAX))).isNull()
    }

    @Test
    fun `never throws for checksum-valid mutations`() {
        val random = Random(1940)
        val interpretedIndexes =
            listOf(
                0 until NativeCrashSnapshotLayout.MODULES_OFFSET,
                NativeCrashSnapshotLayout.MODULES_OFFSET until MODULE_OFFSET + NativeCrashSnapshotLayout.MODULE_ENTRY_SIZE,
                NativeCrashSnapshotLayout.RESERVED_OFFSET until NativeCrashSnapshotLayout.CHECKSUM_OFFSET,
            ).flatMap { it }
        repeat(500) { iteration ->
            val architecture = NativeCrashArchitecture.entries[iteration % NativeCrashArchitecture.entries.size]
            val bytes = SnapshotBuilder(architecture).build()
            val index = interpretedIndexes[random.nextInt(interpretedIndexes.size)]
            bytes[index] = (bytes[index].toInt() xor (1 shl random.nextInt(8))).toByte()
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(
                NativeCrashSnapshotLayout.CHECKSUM_OFFSET,
                NativeCrashSnapshotParser.checksum(bytes).toInt(),
            )

            assertThat(runCatching { NativeCrashSnapshotParser.parse(bytes, crashRecord) }.exceptionOrNull()).isNull()
        }
    }

    @TestFactory
    fun `rejects structurally invalid snapshots`() =
        listOf<Pair<String, (ByteBuffer) -> Unit>>(
            "magic" to { it.put(0, 'X'.code.toByte()) },
            "version" to { it.putInt(NativeCrashSnapshotLayout.VERSION_OFFSET, 2) },
            "architecture" to { it.putInt(NativeCrashSnapshotLayout.ARCHITECTURE_OFFSET, 99) },
            "record size" to { it.putInt(NativeCrashSnapshotLayout.RECORD_SIZE_OFFSET, 1) },
            "zero timestamp" to { it.putLong(NativeCrashSnapshotLayout.TIMESTAMP_OFFSET, 0) },
            "zero stack pointer" to { it.putLong(NativeCrashSnapshotLayout.STACK_POINTER_OFFSET, 0) },
            "different stack start" to { it.putLong(NativeCrashSnapshotLayout.STACK_START_OFFSET, STACK_START + 8) },
            "unaligned stack" to {
                it.putLong(NativeCrashSnapshotLayout.STACK_POINTER_OFFSET, STACK_START + 1)
                it.putLong(NativeCrashSnapshotLayout.STACK_START_OFFSET, STACK_START + 1)
            },
            "zero modules" to { it.putInt(NativeCrashSnapshotLayout.MODULE_COUNT_OFFSET, 0) },
            "too many modules" to { it.putInt(NativeCrashSnapshotLayout.MODULE_COUNT_OFFSET, 129) },
            "negative stack size" to { it.putInt(NativeCrashSnapshotLayout.STACK_SIZE_OFFSET, -1) },
            "oversized stack" to { it.putInt(NativeCrashSnapshotLayout.STACK_SIZE_OFFSET, 4097) },
            "reserved header" to { it.putInt(NativeCrashSnapshotLayout.RESERVED_OFFSET, 1) },
        ).map { (name, mutation) ->
            dynamicTest(name) {
                assertThat(parse(mutate = mutation)).isNull()
            }
        }

    @TestFactory
    fun `rejects non-zero upper bits in 32 bit registers`() =
        listOf<Pair<String, (ByteBuffer) -> Unit>>(
            "program counter" to { it.putLong(NativeCrashSnapshotLayout.PROGRAM_COUNTER_OFFSET, 0x1_0000_1120) },
            "frame pointer" to { it.putLong(NativeCrashSnapshotLayout.FRAME_POINTER_OFFSET, 0x1_0000_7010) },
            "link register" to { it.putLong(NativeCrashSnapshotLayout.LINK_REGISTER_OFFSET, 0x1_0000_1130) },
            "stack pointer" to {
                it.putLong(NativeCrashSnapshotLayout.STACK_POINTER_OFFSET, 0x1_0000_7000)
                it.putLong(NativeCrashSnapshotLayout.STACK_START_OFFSET, 0x1_0000_7000)
            },
        ).map { (name, mutation) ->
            dynamicTest(name) {
                assertThat(parse(NativeCrashArchitecture.ARM, mutation)).isNull()
            }
        }

    @TestFactory
    fun `skips malformed module entries`() =
        // Raw relative offsets intentionally cross-check the mirrored binary layout constants.
        listOf<Pair<String, (ByteBuffer) -> Unit>>(
            "load bias after start" to { it.putLong(MODULE_OFFSET, 0x1200) },
            "empty executable range" to { it.putLong(MODULE_OFFSET + 16, 0x1100) },
            "missing name terminator" to { it.fill(MODULE_OFFSET + 24, 64, 'a'.code.toByte()) },
            "empty name" to { it.fill(MODULE_OFFSET + 24, 64, 0) },
            "name padding" to { it.put(MODULE_OFFSET + 24 + 20, 1) },
            "malformed utf8" to {
                it.put(MODULE_OFFSET + 24, 0xc3.toByte())
                it.put(MODULE_OFFSET + 25, 0x28)
            },
            "control character" to { it.put(MODULE_OFFSET + 27, '\n'.code.toByte()) },
            "path instead of basename" to { it.put(MODULE_OFFSET + 27, '/'.code.toByte()) },
            "negative build id length" to { it.putInt(MODULE_OFFSET + 88, -1) },
            "oversized build id" to { it.putInt(MODULE_OFFSET + 88, 33) },
            "build id padding" to { it.put(MODULE_OFFSET + 102, 1) },
            "reserved module" to { it.putInt(MODULE_OFFSET + 124, 1) },
        ).map { (name, mutation) ->
            dynamicTest(name) {
                assertThat(requireNotNull(parse(mutate = mutation)).modules).isEmpty()
            }
        }

    @Test
    fun `keeps valid modules after a malformed entry`() {
        val snapshot =
            requireNotNull(
                parse(NativeCrashArchitecture.X86) { buffer ->
                    buffer.putInt(NativeCrashSnapshotLayout.MODULE_COUNT_OFFSET, 2)
                    buffer.putLong(MODULE_OFFSET, 0x1_0000_1000)
                    buffer.writeModule(MODULE_OFFSET + 128, 0x3000, 0x3100, 0x4000, "libsecond.so")
                },
            )

        assertThat(snapshot.modules)
            .containsExactly(NativeCrashModule(0x3000UL, 0x3100UL, 0x4000UL, "libsecond.so", null))
    }

    private fun parse(
        architecture: NativeCrashArchitecture = NativeCrashArchitecture.ARM64,
        mutate: (ByteBuffer) -> Unit = {},
    ): NativeCrashSnapshot? = NativeCrashSnapshotParser.parse(SnapshotBuilder(architecture).build(mutate), crashRecord)

    private inner class SnapshotBuilder(
        private val architecture: NativeCrashArchitecture = NativeCrashArchitecture.ARM64,
    ) {
        fun build(mutate: (ByteBuffer) -> Unit = {}): ByteArray {
            val bytes = ByteArray(NativeCrashSnapshotLayout.RECORD_SIZE)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("OTELNCS\u0000".toByteArray())
            buffer.putInt(1)
            buffer.putInt(architecture.id)
            buffer.putInt(bytes.size)
            buffer.putInt(crashRecord.signalNumber)
            buffer.putLong(TIMESTAMP_NANOS)
            buffer.putLong(0x1120)
            buffer.putLong(STACK_START)
            buffer.putLong(STACK_START + 16)
            buffer.putLong(if (architecture.hasLinkRegister) 0x1130 else 0)
            buffer.putInt(1)
            buffer.putInt(4)
            buffer.putLong(STACK_START)
            buffer.writeModule(MODULE_OFFSET, 0x1000, 0x1100, 0x2000, "libapp.so", byteArrayOf(1, 0x23, 0xfe.toByte()))
            buffer.position(NativeCrashSnapshotLayout.STACK_OFFSET)
            buffer.put(byteArrayOf(1, 2, 3, 4))
            buffer.put(NativeCrashSnapshotLayout.STACK_OFFSET + 100, 9)
            mutate(buffer)
            buffer.putInt(NativeCrashSnapshotLayout.CHECKSUM_OFFSET, NativeCrashSnapshotParser.checksum(bytes).toInt())
            return bytes
        }
    }

    private fun ByteBuffer.writeModule(
        offset: Int,
        loadBias: Long,
        start: Long,
        end: Long,
        name: String,
        buildId: ByteArray = byteArrayOf(),
    ) {
        putLong(offset, loadBias)
        putLong(offset + 8, start)
        putLong(offset + 16, end)
        position(offset + 24)
        put(name.toByteArray())
        putInt(offset + 88, buildId.size)
        position(offset + 92)
        put(buildId)
    }

    private fun ByteBuffer.writeMaximumModuleTable() {
        putInt(NativeCrashSnapshotLayout.MODULE_COUNT_OFFSET, NativeCrashSnapshotLayout.MAX_MODULES)
        for (index in 1 until NativeCrashSnapshotLayout.MAX_MODULES) {
            val executableStart = 0x3000L + index * 0x100L
            writeModule(
                MODULE_OFFSET + index * NativeCrashSnapshotLayout.MODULE_ENTRY_SIZE,
                executableStart - 0x10,
                executableStart,
                executableStart + 0x80,
                "lib$index.so",
            )
        }
    }

    private fun ByteBuffer.fill(
        offset: Int,
        size: Int,
        value: Byte,
    ) = repeat(size) { put(offset + it, value) }

    private companion object {
        const val STACK_START = 0x7000L
        const val MODULE_OFFSET = NativeCrashSnapshotLayout.MODULES_OFFSET
        const val TIMESTAMP_NANOS = 1_783_598_400_123_456_789L
        val crashRecord = NativeCrashRecord(11, Instant.ofEpochSecond(1_783_598_400, 123_456_789))
    }
}
