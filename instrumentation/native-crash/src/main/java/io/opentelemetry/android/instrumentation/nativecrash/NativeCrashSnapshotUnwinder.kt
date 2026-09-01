/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

internal enum class NativeCrashFrameOrigin {
    PROGRAM_COUNTER,
    FRAME_POINTER,
    LINK_REGISTER,
}

internal data class NativeCrashFrame(
    val moduleName: String,
    val moduleRelativeAddress: ULong,
    val buildId: String?,
    val origin: NativeCrashFrameOrigin,
)

internal object NativeCrashSnapshotUnwinder {
    private const val MAX_FRAMES = 64
    private const val ARM64_CLEAR_BITS_56_TO_63 = 0x00ff_ffff_ffff_ffffUL
    private const val ARM64_CLEAR_BITS_52_TO_63 = 0x000f_ffff_ffff_ffffUL
    private const val ARM64_CLEAR_BITS_48_TO_63 = 0x0000_ffff_ffff_ffffUL
    private const val ARM64_CLEAR_BITS_47_TO_63 = 0x0000_7fff_ffff_ffffUL
    private const val ARM64_CLEAR_BITS_39_TO_63 = 0x0000_007f_ffff_ffffUL
    private const val ARM_CLEAR_THUMB_BIT = 0xffff_ffff_ffff_fffeUL

    fun unwind(snapshot: NativeCrashSnapshot): List<NativeCrashFrame> {
        val frames = ArrayList<NativeCrashFrame>(MAX_FRAMES)
        snapshot.resolve(snapshot.programCounter, NativeCrashFrameOrigin.PROGRAM_COUNTER)?.let(frames::add)

        val callerCount =
            if (snapshot.architecture == NativeCrashArchitecture.ARM) {
                0
            } else {
                walkFramePointers(snapshot, frames)
            }

        if (callerCount == 0 && snapshot.architecture.hasLinkRegister) {
            snapshot.resolve(snapshot.linkRegister, NativeCrashFrameOrigin.LINK_REGISTER)?.let(frames::add)
        }
        return frames
    }

    private fun walkFramePointers(
        snapshot: NativeCrashSnapshot,
        frames: MutableList<NativeCrashFrame>,
    ): Int {
        var framePointer = snapshot.framePointer
        var recoveredCallers = 0
        while (frames.size < MAX_FRAMES) {
            val record = snapshot.readFrameRecord(framePointer) ?: return recoveredCallers
            snapshot.resolve(record.returnAddress, NativeCrashFrameOrigin.FRAME_POINTER)?.let {
                frames.add(it)
                recoveredCallers++
            }
            if (record.previousFramePointer <= framePointer) return recoveredCallers
            framePointer = record.previousFramePointer
        }
        return recoveredCallers
    }

    private fun NativeCrashSnapshot.resolve(
        address: ULong,
        origin: NativeCrashFrameOrigin,
    ): NativeCrashFrame? {
        if (address == 0UL) return null
        for (candidate in addressCandidates(address)) {
            val module =
                modules.firstOrNull {
                    it.loadBias <= candidate &&
                        candidate >= it.executableStart &&
                        candidate < it.executableEnd
                } ?: continue
            return NativeCrashFrame(
                moduleName = module.name,
                moduleRelativeAddress = candidate - module.loadBias,
                buildId = module.buildId,
                origin = origin,
            )
        }
        return null
    }

    private fun NativeCrashSnapshot.addressCandidates(address: ULong): List<ULong> =
        when (architecture) {
            NativeCrashArchitecture.ARM -> {
                listOf(address and ARM_CLEAR_THUMB_BIT)
            }

            NativeCrashArchitecture.ARM64 -> {
                listOf(
                    address,
                    address and ARM64_CLEAR_BITS_56_TO_63,
                    address and ARM64_CLEAR_BITS_52_TO_63,
                    address and ARM64_CLEAR_BITS_48_TO_63,
                    address and ARM64_CLEAR_BITS_47_TO_63,
                    address and ARM64_CLEAR_BITS_39_TO_63,
                ).distinct()
            }

            NativeCrashArchitecture.X86,
            NativeCrashArchitecture.X86_64,
            -> {
                listOf(address)
            }
        }

    private fun NativeCrashSnapshot.readFrameRecord(framePointer: ULong): FrameRecord? {
        val pointerSize = architecture.pointerSize
        if (framePointer % pointerSize.toULong() != 0UL) return null
        val returnAddressLocation = framePointer.addWithoutOverflow(pointerSize.toULong()) ?: return null
        val previousFramePointer = readStackPointer(framePointer) ?: return null
        val returnAddress = readStackPointer(returnAddressLocation) ?: return null
        return FrameRecord(previousFramePointer, returnAddress)
    }

    private fun NativeCrashSnapshot.readStackPointer(address: ULong): ULong? {
        if (address < stackStart) return null
        val offset = address - stackStart
        val pointerSize = architecture.pointerSize.toULong()
        val stackSize = stack.size.toULong()
        if (offset > stackSize || pointerSize > stackSize - offset) return null
        val index = offset.toInt()
        var value = 0UL
        repeat(architecture.pointerSize) { byteIndex ->
            value = value or (stack[index + byteIndex].toUByte().toULong() shl (byteIndex * Byte.SIZE_BITS))
        }
        return value
    }

    private fun ULong.addWithoutOverflow(value: ULong): ULong? = takeIf { it <= ULong.MAX_VALUE - value }?.plus(value)

    private data class FrameRecord(
        val previousFramePointer: ULong,
        val returnAddress: ULong,
    )
}
