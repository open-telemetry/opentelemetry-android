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
        val programCounterFrame = snapshot.resolve(snapshot.programCounter, NativeCrashFrameOrigin.PROGRAM_COUNTER)
        programCounterFrame?.let(frames::add)

        val callerFrames =
            if (snapshot.architecture == NativeCrashArchitecture.ARM) {
                emptyList()
            } else {
                walkFramePointers(snapshot)
            }
        val linkRegisterFrame =
            snapshot
                .takeIf { it.architecture.hasLinkRegister }
                ?.resolve(snapshot.linkRegister, NativeCrashFrameOrigin.LINK_REGISTER)
        if (linkRegisterFrame != null) {
            val fillsMissingCaller = programCounterFrame == null || callerFrames.isEmpty()
            val duplicatesFrame =
                frames.any { it.sameLocationAs(linkRegisterFrame) } ||
                    callerFrames.any { it.sameLocationAs(linkRegisterFrame) }
            if (fillsMissingCaller && !duplicatesFrame) frames.add(linkRegisterFrame)
        }
        for (callerFrame in callerFrames) {
            if (frames.size == MAX_FRAMES) break
            frames.add(callerFrame)
        }
        return frames
    }

    private fun walkFramePointers(snapshot: NativeCrashSnapshot): List<NativeCrashFrame> {
        val frames = ArrayList<NativeCrashFrame>(MAX_FRAMES)
        var framePointer = snapshot.framePointer
        var remainingRecords =
            minOf(snapshot.stack.size, NativeCrashSnapshotLayout.STACK_CAPACITY) /
                snapshot.architecture.pointerSize
        while (frames.size < MAX_FRAMES && remainingRecords > 0) {
            remainingRecords--
            val record = snapshot.readFrameRecord(framePointer) ?: return frames
            snapshot.resolve(record.returnAddress, NativeCrashFrameOrigin.FRAME_POINTER)?.let {
                frames.add(it)
            }
            val previousLocation = snapshot.locateFrameRecord(record.previousFramePointer) ?: return frames
            if (previousLocation.offset <= record.offset) return frames
            framePointer = record.previousFramePointer
        }
        return frames
    }

    private fun NativeCrashSnapshot.resolve(
        address: ULong,
        origin: NativeCrashFrameOrigin,
    ): NativeCrashFrame? {
        if (address == 0UL) return null
        val returnAddressAdjustment = returnAddressAdjustment(address, origin)
        for (candidate in addressCandidates(address)) {
            if (candidate >= returnAddressAdjustment) {
                val instructionAddress = candidate - returnAddressAdjustment
                val module =
                    modules.firstOrNull {
                        it.loadBias <= instructionAddress &&
                            instructionAddress >= it.executableStart &&
                            instructionAddress < it.executableEnd
                    }
                if (module != null) {
                    return NativeCrashFrame(
                        moduleName = module.name,
                        moduleRelativeAddress = instructionAddress - module.loadBias,
                        buildId = module.buildId,
                        origin = origin,
                    )
                }
            }
        }
        return null
    }

    private fun NativeCrashSnapshot.returnAddressAdjustment(
        address: ULong,
        origin: NativeCrashFrameOrigin,
    ): ULong {
        if (origin == NativeCrashFrameOrigin.PROGRAM_COUNTER) return 0UL
        return when (architecture) {
            NativeCrashArchitecture.ARM -> if (address and 1UL == 1UL) 2UL else 4UL

            NativeCrashArchitecture.ARM64 -> 4UL

            NativeCrashArchitecture.X86,
            NativeCrashArchitecture.X86_64,
            -> 1UL
        }
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
        val location = locateFrameRecord(framePointer) ?: return null
        val previousFramePointer = readStackPointer(location.offset)
        val returnAddress = readStackPointer(location.offset + pointerSize)
        return FrameRecord(location.offset, previousFramePointer, returnAddress)
    }

    private fun NativeCrashSnapshot.locateFrameRecord(address: ULong): StackLocation? {
        val pointerSize = architecture.pointerSize.toULong()
        val recordSize = pointerSize * 2UL
        val stackSize = stack.size.toULong()
        for ((candidate, candidateStackStart) in stackAddressCandidates(address, stackStart)) {
            val recordEndDoesNotOverflow = candidate <= ULong.MAX_VALUE - (recordSize - 1UL)
            if (candidate % pointerSize == 0UL && recordEndDoesNotOverflow && candidate >= candidateStackStart) {
                val offset = candidate - candidateStackStart
                if (offset <= stackSize && recordSize <= stackSize - offset) {
                    return StackLocation(offset.toInt())
                }
            }
        }
        return null
    }

    private fun NativeCrashSnapshot.stackAddressCandidates(
        address: ULong,
        stackStart: ULong,
    ): List<Pair<ULong, ULong>> =
        if (architecture == NativeCrashArchitecture.ARM64) {
            listOf(
                ULong.MAX_VALUE,
                ARM64_CLEAR_BITS_56_TO_63,
                ARM64_CLEAR_BITS_52_TO_63,
                ARM64_CLEAR_BITS_48_TO_63,
                ARM64_CLEAR_BITS_47_TO_63,
                ARM64_CLEAR_BITS_39_TO_63,
            ).map { mask -> (address and mask) to (stackStart and mask) }.distinct()
        } else {
            listOf(address to stackStart)
        }

    private fun NativeCrashSnapshot.readStackPointer(offset: Int): ULong {
        var value = 0UL
        repeat(architecture.pointerSize) { byteIndex ->
            value = value or (stack[offset + byteIndex].toUByte().toULong() shl (byteIndex * Byte.SIZE_BITS))
        }
        return value
    }

    private fun NativeCrashFrame.sameLocationAs(other: NativeCrashFrame): Boolean =
        moduleName == other.moduleName && moduleRelativeAddress == other.moduleRelativeAddress && buildId == other.buildId

    private data class FrameRecord(
        val offset: Int,
        val previousFramePointer: ULong,
        val returnAddress: ULong,
    )

    private data class StackLocation(
        val offset: Int,
    )
}
