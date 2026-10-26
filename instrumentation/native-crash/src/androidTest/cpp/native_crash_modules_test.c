/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

#include "../../main/cpp/native_crash_modules.h"

#include <errno.h>
#include <jni.h>
#include <string.h>

typedef int (*module_callback)(struct dl_phdr_info *, size_t, void *);

static int emit(module_callback callback, void *data, const char *name, uintptr_t bias) {
    ElfW(Phdr) headers[] = {
        {.p_type = PT_LOAD, .p_flags = PF_R | PF_X, .p_vaddr = 0x100, .p_memsz = 0x80},
        {.p_type = PT_LOAD, .p_flags = PF_R, .p_vaddr = 0x200, .p_memsz = 0x80},
        {.p_type = PT_NOTE, .p_flags = PF_X, .p_vaddr = 0x300, .p_memsz = 0x80},
        {.p_type = PT_LOAD, .p_flags = PF_X, .p_vaddr = 0x400, .p_memsz = 0},
    };
    struct dl_phdr_info info = {
        .dlpi_addr = bias, .dlpi_name = name, .dlpi_phdr = headers, .dlpi_phnum = 4,
    };
    return callback(&info, sizeof(info), data);
}

static int ordered(module_callback callback, void *data) {
    emit(callback, data, "/system/lib/libsystem.so", 0x10000);
    emit(callback, data, "/data/app/a/base.apk!/lib/arm64-v8a/libapp.so", 0x20000);
    emit(callback, data, "/apex/runtime/lib/libapex.so", 0x30000);
    emit(callback, data, "/mnt/expand/volume/app/a/libadopted.so", 0x40000);
    return 0;
}

static int ranges(module_callback callback, void *data) {
    emit(callback, data, "libwrapped-start.so", UINTPTR_MAX - 0xff);
    emit(callback, data, "libwrapped-end.so", UINTPTR_MAX - 0x100);
    emit(callback, data, "libhighest.so", UINTPTR_MAX - 0x180);
    ElfW(Phdr) headers[] = {
        {.p_type = PT_LOAD, .p_flags = PF_X, .p_vaddr = 0x100, .p_memsz = 0x100},
        {.p_type = PT_LOAD, .p_flags = PF_X, .p_vaddr = 0x400, .p_memsz = 0x100},
    };
    struct dl_phdr_info info = {.dlpi_name = "libmultiple.so", .dlpi_phdr = headers, .dlpi_phnum = 2};
    callback(&info, sizeof(info), data);
    return 0;
}

static int names(module_callback callback, void *data) {
    const char *invalid[] = {
        NULL, "", "/", " ", "\xc2\xa0", "\xe3\x80\x80", "lib\n.so", "lib\xc2\x85.so",
        "\xc0\xaf", "\xe0\x80\xaf", "\xed\xa0\x80", "\xf4\x90\x80\x80", "lib\xc3", "\x80",
    };
    for (size_t index = 0; index < sizeof(invalid) / sizeof(*invalid); index++) {
        emit(callback, data, invalid[index], 0x10000);
    }
    char name[OTEL_NCS_MODULE_NAME_SIZE + 1];
    memset(name, 'a', sizeof(name));
    name[sizeof(name) - 1] = '\0';
    emit(callback, data, name, 0x10000);
    name[sizeof(name) - 2] = '\0';
    emit(callback, data, name, 0x20000);
    emit(callback, data, "/data/app/lib\xc3\xa9.so", 0x30000);
    emit(callback, data, "lib\xf4\x8f\xbf\xbf.so", 0x40000);
    return 0;
}

static int full_system_table(module_callback callback, void *data) {
    for (uintptr_t index = 0; index < OTEL_NCS_MAX_MODULES + 2; index++) {
        emit(callback, data, "/system/lib/libsystem.so", index * 0x1000);
    }
    emit(callback, data, "/data/app/libfirst.so", 0x200000);
    emit(callback, data, "/data/app/libsecond.so", 0x300000);
    return 0;
}

static int full_app_table(module_callback callback, void *data) {
    for (uintptr_t index = 0; index < OTEL_NCS_MAX_MODULES + 2; index++) {
        emit(callback, data, "/data/app/libapp.so", index * 0x1000);
    }
    return emit(callback, data, "/system/lib/libsystem.so", 0x300000);
}

static int old_info(module_callback callback, void *data) {
    ElfW(Phdr) header = {.p_type = PT_LOAD, .p_flags = PF_X, .p_vaddr = 0x100, .p_memsz = 0x80};
    struct dl_phdr_info info = {.dlpi_name = "libold.so", .dlpi_phdr = &header, .dlpi_phnum = 1};
    size_t size = offsetof(struct dl_phdr_info, dlpi_phnum) + sizeof(info.dlpi_phnum);
    callback(NULL, size, data);
    callback(&info, size - 1, data);
    callback(&info, size, data);
    info.dlpi_phdr = NULL;
    callback(&info, size, data);
    return 0;
}

static int failing(module_callback callback, void *data) {
    emit(callback, data, "/data/app/libapp.so", 0x10000);
    errno = EIO;
    return -1;
}

static int empty(module_callback callback, void *data) {
    return emit(callback, data, "", 0x10000);
}

JNIEXPORT jbyteArray JNICALL
Java_io_opentelemetry_android_instrumentation_nativecrash_NativeCrashModuleTestJni_snapshot(
    JNIEnv *environment, jclass clazz, jint scenario) {
    (void) clazz;
    otel_module_iterator iterators[] = {
        ordered, ranges, names, full_system_table, full_app_table, old_info, failing, NULL, dl_iterate_phdr, empty,
    };
    if (scenario < 0 || (size_t) scenario >= sizeof(iterators) / sizeof(*iterators)) return NULL;
    // Only the modules come from the production helper. The envelope is a synthetic crash fixture.
    struct otel_native_crash_snapshot snapshot = {
        .magic = OTEL_NCS_MAGIC, .version = OTEL_NCS_VERSION, .architecture = OTEL_NCS_ARCH_CURRENT,
        .record_size = sizeof(snapshot), .signal_number = 11, .timestamp_epoch_nanos = 1000000000,
        .stack_pointer = 0x8000, .stack_start = 0x8000,
    };
    struct {
        uint64_t before;
        struct otel_native_crash_module modules[OTEL_NCS_MAX_MODULES];
        uint64_t after;
    } guarded;
    memset(&guarded, 0xa5, sizeof(guarded));
    errno = EDOM;
    snapshot.module_count = otel_native_crash_prepare_modules(guarded.modules, iterators[scenario]);
    if (errno != EDOM || guarded.before != UINT64_C(0xa5a5a5a5a5a5a5a5) ||
        guarded.after != UINT64_C(0xa5a5a5a5a5a5a5a5) ||
        otel_native_crash_prepare_modules(NULL, iterators[scenario]) != 0) return NULL;
    memcpy(snapshot.modules, guarded.modules, sizeof(snapshot.modules));
    if (scenario == 8) snapshot.program_counter = (uintptr_t) &otel_native_crash_prepare_modules;
    snapshot.checksum = OTEL_NCS_FNV_OFFSET_BASIS;
    const unsigned char *bytes = (const unsigned char *) &snapshot;
    for (size_t index = 0; index < offsetof(struct otel_native_crash_snapshot, checksum); index++) {
        snapshot.checksum = (snapshot.checksum ^ bytes[index]) * OTEL_NCS_FNV_PRIME;
    }
    jbyteArray result = (*environment)->NewByteArray(environment, sizeof(snapshot));
    if (result != NULL) {
        (*environment)->SetByteArrayRegion(environment, result, 0, sizeof(snapshot), (const jbyte *) bytes);
    }
    return result;
}

JNIEXPORT jlong JNICALL
Java_io_opentelemetry_android_instrumentation_nativecrash_NativeCrashModuleTestJni_functionAddress(
    JNIEnv *environment, jclass clazz) {
    (void) environment;
    (void) clazz;
    return (jlong) (uintptr_t) &otel_native_crash_prepare_modules;
}
