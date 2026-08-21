/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef OTEL_ANDROID_NATIVE_CRASH_SNAPSHOT_H
#define OTEL_ANDROID_NATIVE_CRASH_SNAPSHOT_H

#include <stddef.h>
#include <stdint.h>

#define OTEL_NCS_MAGIC_SIZE 8
#define OTEL_NCS_MAGIC {'O', 'T', 'E', 'L', 'N', 'C', 'S', '\0'}
#define OTEL_NCS_VERSION UINT32_C(1)
#define OTEL_NCS_FNV_OFFSET_BASIS UINT32_C(0x811C9DC5)
#define OTEL_NCS_FNV_PRIME UINT32_C(0x01000193)
#define OTEL_NCS_MAX_MODULES 128
#define OTEL_NCS_MODULE_NAME_SIZE 64
#define OTEL_NCS_BUILD_ID_SIZE 32
#define OTEL_NCS_STACK_CAPACITY 4096

#define OTEL_NCS_ARCH_ARM UINT32_C(1)
#define OTEL_NCS_ARCH_ARM64 UINT32_C(2)
#define OTEL_NCS_ARCH_X86 UINT32_C(3)
#define OTEL_NCS_ARCH_X86_64 UINT32_C(4)

#if defined(__arm__)
#define OTEL_NCS_ARCH_CURRENT OTEL_NCS_ARCH_ARM
#elif defined(__aarch64__)
#define OTEL_NCS_ARCH_CURRENT OTEL_NCS_ARCH_ARM64
#elif defined(__i386__)
#define OTEL_NCS_ARCH_CURRENT OTEL_NCS_ARCH_X86
#elif defined(__x86_64__)
#define OTEL_NCS_ARCH_CURRENT OTEL_NCS_ARCH_X86_64
#else
#error "Unsupported architecture for native crash snapshots"
#endif

// The binary layout is documented in SNAPSHOT_FORMAT.md. Keep both in sync.
struct otel_native_crash_module {
    uint64_t load_bias;
    uint64_t executable_start;
    uint64_t executable_end;
    char name[OTEL_NCS_MODULE_NAME_SIZE];
    uint32_t build_id_size;
    unsigned char build_id[OTEL_NCS_BUILD_ID_SIZE];
    uint32_t reserved;
};

struct otel_native_crash_snapshot {
    unsigned char magic[OTEL_NCS_MAGIC_SIZE];
    uint32_t version;
    uint32_t architecture;
    uint32_t record_size;
    uint32_t signal_number;
    uint64_t timestamp_epoch_nanos;
    uint64_t program_counter;
    uint64_t stack_pointer;
    uint64_t frame_pointer;
    uint64_t link_register;
    uint32_t module_count;
    uint32_t stack_size;
    uint64_t stack_start;
    struct otel_native_crash_module modules[OTEL_NCS_MAX_MODULES];
    unsigned char stack[OTEL_NCS_STACK_CAPACITY];
    uint32_t reserved;
    uint32_t checksum;
};

_Static_assert(offsetof(struct otel_native_crash_module, load_bias) == 0, "Unexpected load bias offset");
_Static_assert(offsetof(struct otel_native_crash_module, executable_start) == 8, "Unexpected executable start offset");
_Static_assert(offsetof(struct otel_native_crash_module, executable_end) == 16, "Unexpected executable end offset");
_Static_assert(offsetof(struct otel_native_crash_module, name) == 24, "Unexpected module name offset");
_Static_assert(offsetof(struct otel_native_crash_module, build_id_size) == 88, "Unexpected build ID size offset");
_Static_assert(offsetof(struct otel_native_crash_module, build_id) == 92, "Unexpected build ID offset");
_Static_assert(offsetof(struct otel_native_crash_module, reserved) == 124, "Unexpected module reserved offset");
_Static_assert(sizeof(struct otel_native_crash_module) == 128, "Unexpected module layout");
_Static_assert(OTEL_NCS_MAGIC_SIZE == 8, "Unexpected magic size");
_Static_assert(offsetof(struct otel_native_crash_snapshot, magic) == 0, "Unexpected magic offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, version) == 8, "Unexpected version offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, architecture) == 12, "Unexpected architecture offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, record_size) == 16, "Unexpected record size offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, signal_number) == 20, "Unexpected signal number offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, timestamp_epoch_nanos) == 24, "Unexpected timestamp offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, program_counter) == 32, "Unexpected program counter offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, stack_pointer) == 40, "Unexpected stack pointer offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, frame_pointer) == 48, "Unexpected frame pointer offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, link_register) == 56, "Unexpected link register offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, module_count) == 64, "Unexpected module count offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, stack_size) == 68, "Unexpected stack size offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, stack_start) == 72, "Unexpected stack start offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, modules) == 80, "Unexpected header layout");
_Static_assert(offsetof(struct otel_native_crash_snapshot, stack) == 16464, "Unexpected stack offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, reserved) == 20560, "Unexpected reserved offset");
_Static_assert(offsetof(struct otel_native_crash_snapshot, checksum) == 20564, "Unexpected checksum offset");
_Static_assert(sizeof(struct otel_native_crash_snapshot) == 20568, "Unexpected record size");

#endif
