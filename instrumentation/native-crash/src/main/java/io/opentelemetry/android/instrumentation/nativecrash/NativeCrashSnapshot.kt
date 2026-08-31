/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CodingErrorAction

// Mirrors native_crash_snapshot.h, which remains the binary layout source of truth.
internal object NativeCrashSnapshotLayout {
    const val MAGIC_SIZE = 8
    const val VERSION_OFFSET = 8
    const val ARCHITECTURE_OFFSET = 12
    const val RECORD_SIZE_OFFSET = 16
    const val SIGNAL_NUMBER_OFFSET = 20
    const val TIMESTAMP_OFFSET = 24
    const val PROGRAM_COUNTER_OFFSET = 32
    const val STACK_POINTER_OFFSET = 40
    const val FRAME_POINTER_OFFSET = 48
    const val LINK_REGISTER_OFFSET = 56
    const val MODULE_COUNT_OFFSET = 64
    const val STACK_SIZE_OFFSET = 68
    const val STACK_START_OFFSET = 72
    const val MODULES_OFFSET = 80

    const val MAX_MODULES = 128
    const val MODULE_ENTRY_SIZE = 128
    const val MODULE_LOAD_BIAS_OFFSET = 0
    const val MODULE_EXECUTABLE_START_OFFSET = 8
    const val MODULE_EXECUTABLE_END_OFFSET = 16
    const val MODULE_NAME_OFFSET = 24
    const val MODULE_NAME_SIZE = 64
    const val MODULE_BUILD_ID_SIZE_OFFSET = 88
    const val MODULE_BUILD_ID_OFFSET = 92
    const val MODULE_BUILD_ID_CAPACITY = 32
    const val MODULE_RESERVED_OFFSET = 124

    const val STACK_OFFSET = MODULES_OFFSET + MAX_MODULES * MODULE_ENTRY_SIZE
    const val STACK_CAPACITY = 4_096
    const val RESERVED_OFFSET = STACK_OFFSET + STACK_CAPACITY
    const val CHECKSUM_OFFSET = RESERVED_OFFSET + Int.SIZE_BYTES
    const val RECORD_SIZE = CHECKSUM_OFFSET + Int.SIZE_BYTES
}

internal enum class NativeCrashArchitecture(
    val id: Int,
    val pointerSize: Int,
    val hasLinkRegister: Boolean,
) {
    ARM(1, Int.SIZE_BYTES, true),
    ARM64(2, Long.SIZE_BYTES, true),
    X86(3, Int.SIZE_BYTES, false),
    X86_64(4, Long.SIZE_BYTES, false),
    ;

    companion object {
        fun fromId(id: Int): NativeCrashArchitecture? = entries.firstOrNull { it.id == id }
    }
}

internal class NativeCrashSnapshot(
    val architecture: NativeCrashArchitecture,
    val programCounter: ULong,
    val stackPointer: ULong,
    val framePointer: ULong,
    val linkRegister: ULong,
    val modules: List<NativeCrashModule>,
    val stackStart: ULong,
    val stack: ByteArray,
)

internal data class NativeCrashModule(
    val loadBias: ULong,
    val executableStart: ULong,
    val executableEnd: ULong,
    val name: String,
    val buildId: String?,
)

internal object NativeCrashSnapshotParser {
    private const val VERSION = 1
    private const val FNV_OFFSET_BASIS = 0x811c9dc5u
    private const val FNV_PRIME = 0x01000193u
    private const val NANOS_PER_SECOND = 1_000_000_000L
    private const val HEX_DIGITS = "0123456789abcdef"

    private val magic = "OTELNCS\u0000".toByteArray(Charsets.US_ASCII)

    fun parse(
        bytes: ByteArray,
        crashRecord: NativeCrashRecord,
    ): NativeCrashSnapshot? {
        if (!hasValidEnvelope(bytes)) return null

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val architecture = readValidatedArchitecture(bytes, buffer, crashRecord) ?: return null

        val programCounter = buffer.addressAt(NativeCrashSnapshotLayout.PROGRAM_COUNTER_OFFSET, architecture) ?: return null
        val stackPointer = buffer.addressAt(NativeCrashSnapshotLayout.STACK_POINTER_OFFSET, architecture) ?: return null
        val framePointer = buffer.addressAt(NativeCrashSnapshotLayout.FRAME_POINTER_OFFSET, architecture) ?: return null
        val linkRegister = buffer.linkRegister(architecture) ?: return null
        val stackStart = buffer.addressAt(NativeCrashSnapshotLayout.STACK_START_OFFSET, architecture) ?: return null
        val moduleCount = buffer.getInt(NativeCrashSnapshotLayout.MODULE_COUNT_OFFSET)
        val stackSize = buffer.getInt(NativeCrashSnapshotLayout.STACK_SIZE_OFFSET)

        if (stackPointer == 0UL || stackStart != stackPointer) return null
        if (stackStart % architecture.pointerSize.toULong() != 0UL) return null
        if (moduleCount !in 1..NativeCrashSnapshotLayout.MAX_MODULES) return null
        if (stackSize !in 0..NativeCrashSnapshotLayout.STACK_CAPACITY) return null
        if (buffer.getInt(NativeCrashSnapshotLayout.RESERVED_OFFSET) != 0) return null

        val modules =
            (0 until moduleCount).mapNotNull { index ->
                readModule(bytes, buffer, index, architecture)
            }
        val stack =
            bytes.copyOfRange(
                NativeCrashSnapshotLayout.STACK_OFFSET,
                NativeCrashSnapshotLayout.STACK_OFFSET + stackSize,
            )
        return NativeCrashSnapshot(
            architecture = architecture,
            programCounter = programCounter,
            stackPointer = stackPointer,
            framePointer = framePointer,
            linkRegister = linkRegister,
            modules = modules,
            stackStart = stackStart,
            stack = stack,
        )
    }

    internal fun checksum(bytes: ByteArray): UInt {
        require(bytes.size >= NativeCrashSnapshotLayout.CHECKSUM_OFFSET)
        var checksum = FNV_OFFSET_BASIS
        for (index in 0 until NativeCrashSnapshotLayout.CHECKSUM_OFFSET) {
            checksum = (checksum xor bytes[index].toUByte().toUInt()) * FNV_PRIME
        }
        return checksum
    }

    private fun hasValidEnvelope(bytes: ByteArray): Boolean =
        bytes.size == NativeCrashSnapshotLayout.RECORD_SIZE &&
            checksum(bytes) == bytes.uintAt(NativeCrashSnapshotLayout.CHECKSUM_OFFSET)

    private fun readValidatedArchitecture(
        bytes: ByteArray,
        buffer: ByteBuffer,
        crashRecord: NativeCrashRecord,
    ): NativeCrashArchitecture? {
        if (!bytes.copyOfRange(0, NativeCrashSnapshotLayout.MAGIC_SIZE).contentEquals(magic)) return null
        if (buffer.getInt(NativeCrashSnapshotLayout.VERSION_OFFSET) != VERSION) return null
        val architecture =
            NativeCrashArchitecture.fromId(buffer.getInt(NativeCrashSnapshotLayout.ARCHITECTURE_OFFSET))
                ?: return null
        if (buffer.getInt(NativeCrashSnapshotLayout.RECORD_SIZE_OFFSET) != NativeCrashSnapshotLayout.RECORD_SIZE) return null
        return architecture.takeIf { matchesCrashRecord(buffer, crashRecord) }
    }

    private fun matchesCrashRecord(
        buffer: ByteBuffer,
        crashRecord: NativeCrashRecord,
    ): Boolean {
        if (buffer.getInt(NativeCrashSnapshotLayout.SIGNAL_NUMBER_OFFSET) != crashRecord.signalNumber) return false
        val timestampNanos = buffer.getLong(NativeCrashSnapshotLayout.TIMESTAMP_OFFSET)
        val epochSecond = crashRecord.timestamp.epochSecond
        val nano = crashRecord.timestamp.nano.toLong()
        if (timestampNanos <= 0 || epochSecond < 0 || epochSecond > (Long.MAX_VALUE - nano) / NANOS_PER_SECOND) return false
        return timestampNanos == epochSecond * NANOS_PER_SECOND + nano
    }

    private fun readModule(
        bytes: ByteArray,
        buffer: ByteBuffer,
        index: Int,
        architecture: NativeCrashArchitecture,
    ): NativeCrashModule? {
        val base = NativeCrashSnapshotLayout.MODULES_OFFSET + index * NativeCrashSnapshotLayout.MODULE_ENTRY_SIZE
        val loadBias = buffer.addressAt(base + NativeCrashSnapshotLayout.MODULE_LOAD_BIAS_OFFSET, architecture) ?: return null
        val executableStart = buffer.addressAt(base + NativeCrashSnapshotLayout.MODULE_EXECUTABLE_START_OFFSET, architecture) ?: return null
        val executableEnd = buffer.addressAt(base + NativeCrashSnapshotLayout.MODULE_EXECUTABLE_END_OFFSET, architecture) ?: return null
        if (loadBias > executableStart || executableStart >= executableEnd) return null
        if (buffer.getInt(base + NativeCrashSnapshotLayout.MODULE_RESERVED_OFFSET) != 0) return null

        val name =
            decodeName(
                bytes,
                base + NativeCrashSnapshotLayout.MODULE_NAME_OFFSET,
                NativeCrashSnapshotLayout.MODULE_NAME_SIZE,
            ) ?: return null
        val buildIdSize = buffer.getInt(base + NativeCrashSnapshotLayout.MODULE_BUILD_ID_SIZE_OFFSET)
        if (buildIdSize !in 0..NativeCrashSnapshotLayout.MODULE_BUILD_ID_CAPACITY) return null
        val buildIdOffset = base + NativeCrashSnapshotLayout.MODULE_BUILD_ID_OFFSET
        if (bytes.hasNonZeroBytes(
                buildIdOffset + buildIdSize,
                buildIdOffset + NativeCrashSnapshotLayout.MODULE_BUILD_ID_CAPACITY,
            )
        ) {
            return null
        }
        val buildId = buildIdSize.takeIf { it > 0 }?.let { bytes.toHex(buildIdOffset, it) }
        return NativeCrashModule(loadBias, executableStart, executableEnd, name, buildId)
    }

    private fun decodeName(
        bytes: ByteArray,
        offset: Int,
        size: Int,
    ): String? {
        val terminator = (offset until offset + size).firstOrNull { bytes[it] == 0.toByte() } ?: return null
        if (terminator == offset || bytes.hasNonZeroBytes(terminator + 1, offset + size)) return null
        val name =
            runCatching {
                Charsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, terminator - offset))
                    .toString()
            }.getOrNull() ?: return null
        return name.takeUnless { it.isBlank() || it.contains('/') || it.any(Char::isISOControl) }
    }

    private fun ByteBuffer.addressAt(
        offset: Int,
        architecture: NativeCrashArchitecture,
    ): ULong? {
        val address = getLong(offset).toULong()
        return address.takeIf { architecture.pointerSize == Long.SIZE_BYTES || it <= UInt.MAX_VALUE.toULong() }
    }

    private fun ByteBuffer.linkRegister(architecture: NativeCrashArchitecture): ULong? =
        if (architecture.hasLinkRegister) addressAt(NativeCrashSnapshotLayout.LINK_REGISTER_OFFSET, architecture) else 0UL

    private fun ByteArray.uintAt(offset: Int): UInt =
        ByteBuffer
            .wrap(this, offset, Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
            .toUInt()

    private fun ByteArray.hasNonZeroBytes(
        start: Int,
        end: Int,
    ): Boolean = (start until end).any { this[it] != 0.toByte() }

    private fun ByteArray.toHex(
        offset: Int,
        size: Int,
    ): String =
        buildString(size * 2) {
            repeat(size) { index ->
                val value = this@toHex[offset + index].toInt() and 0xff
                append(HEX_DIGITS[value ushr 4])
                append(HEX_DIGITS[value and 0xf])
            }
        }
}
