/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <limits.h>
#include <pthread.h>
#include <signal.h>
#include <stdbool.h>
#include <stdatomic.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/syscall.h>
#include <time.h>
#include <unistd.h>

#include "native_crash_modules.h"

#define SIGNAL_COUNT 7
#define TEMPORARY_SUFFIX ".tmp"
#define MARKER_BUFFER_SIZE 128
#define NANOS_PER_SECOND UINT64_C(1000000000)
#define ALTERNATE_STACK_SIZE (SIGSTKSZ * 2)

#ifndef SA_EXPOSE_TAGBITS
#define SA_EXPOSE_TAGBITS 0x00000800
#endif

static const int handled_signals[SIGNAL_COUNT] = {
    SIGILL,
    SIGTRAP,
    SIGABRT,
    SIGBUS,
    SIGFPE,
    SIGSEGV,
    SIGSYS,
};

static char crash_record_path[PATH_MAX];
static char temporary_crash_record_path[PATH_MAX];
static struct sigaction previous_actions[SIGNAL_COUNT];
static atomic_bool handler_active[SIGNAL_COUNT];
static unsigned char alternate_signal_stack[ALTERNATE_STACK_SIZE];
static pthread_mutex_t install_mutex = PTHREAD_MUTEX_INITIALIZER;
static bool handlers_installed = false;
static bool alternate_signal_stack_installed = false;
static atomic_flag handling_signal = ATOMIC_FLAG_INIT;

struct snapshot_capture_state {
    atomic_bool ready;
    char path[PATH_MAX];
    char temporary_path[PATH_MAX];
    struct otel_native_crash_snapshot snapshot;
};

_Static_assert(ATOMIC_BOOL_LOCK_FREE == 2, "Snapshot publication must be lock-free");
static struct snapshot_capture_state snapshot_capture;

static int find_signal_index(int signal_number) {
    for (int index = 0; index < SIGNAL_COUNT; index++) {
        if (handled_signals[index] == signal_number) {
            return index;
        }
    }
    return -1;
}

static bool append_bytes(
    char *buffer,
    size_t capacity,
    size_t *length,
    const char *value,
    size_t value_length) {
    if (value_length > capacity - *length) {
        return false;
    }
    for (size_t index = 0; index < value_length; index++) {
        buffer[(*length)++] = value[index];
    }
    return true;
}

static bool append_uint64(
    char *buffer,
    size_t capacity,
    size_t *length,
    uint64_t value) {
    char digits[20];
    size_t digit_count = 0;
    do {
        digits[digit_count++] = (char) ('0' + (value % 10));
        value /= 10;
    } while (value != 0);

    if (digit_count > capacity - *length) {
        return false;
    }
    while (digit_count > 0) {
        buffer[(*length)++] = digits[--digit_count];
    }
    return true;
}

static bool write_all(int file_descriptor, const char *buffer, size_t length) {
    size_t written = 0;
    while (written < length) {
        ssize_t result = write(file_descriptor, buffer + written, length - written);
        if (result > 0) {
            written += (size_t) result;
        } else if (result < 0 && errno == EINTR) {
            continue;
        } else {
            return false;
        }
    }
    return true;
}

static bool write_crash_marker_at(
    const char *record_path,
    const char *temporary_record_path,
    int signal_number,
    uint64_t timestamp_epoch_nanos) {
    char marker[MARKER_BUFFER_SIZE];
    size_t marker_length = 0;
    static const char signal_key[] = "signal.number=";
    static const char timestamp_key[] = "\ntimestamp.epoch_nanos=";
    static const char newline[] = "\n";
    if (!append_bytes(marker, sizeof(marker), &marker_length, signal_key, sizeof(signal_key) - 1) ||
        !append_uint64(marker, sizeof(marker), &marker_length, (uint64_t) signal_number) ||
        !append_bytes(
            marker,
            sizeof(marker),
            &marker_length,
            timestamp_key,
            sizeof(timestamp_key) - 1) ||
        !append_uint64(marker, sizeof(marker), &marker_length, timestamp_epoch_nanos) ||
        !append_bytes(marker, sizeof(marker), &marker_length, newline, sizeof(newline) - 1)) {
        return false;
    }

    int file_descriptor =
        open(temporary_record_path, O_CREAT | O_WRONLY | O_TRUNC | O_CLOEXEC, 0600);
    if (file_descriptor < 0) {
        return false;
    }

    bool complete = write_all(file_descriptor, marker, marker_length) && fsync(file_descriptor) == 0;
    if (close(file_descriptor) != 0) {
        complete = false;
    }
    if (!complete || rename(temporary_record_path, record_path) != 0) {
        unlink(temporary_record_path);
        return false;
    }
    return true;
}

static void write_crash_marker(int signal_number) {
    // Snapshot capture must reuse this timestamp; see SNAPSHOT_FORMAT.md.
    struct timespec crash_time;
    if (clock_gettime(CLOCK_REALTIME, &crash_time) != 0 || crash_time.tv_sec < 0 ||
        crash_time.tv_nsec < 0) {
        return;
    }

    uint64_t seconds = (uint64_t) crash_time.tv_sec;
    uint64_t nanoseconds = (uint64_t) crash_time.tv_nsec;
    if (seconds > (UINT64_MAX - nanoseconds) / NANOS_PER_SECOND) {
        return;
    }
    (void) write_crash_marker_at(
        crash_record_path,
        temporary_crash_record_path,
        signal_number,
        seconds * NANOS_PER_SECOND + nanoseconds);
}

static void rollback_installed_handlers(void) {
    for (int index = 0; index < SIGNAL_COUNT; index++) {
        if (atomic_load_explicit(&handler_active[index], memory_order_relaxed)) {
            sigaction(handled_signals[index], &previous_actions[index], NULL);
        }
    }
}

static void remove_alternate_signal_stack(void) {
    if (!alternate_signal_stack_installed) {
        return;
    }
    stack_t disabled_stack;
    memset(&disabled_stack, 0, sizeof(disabled_stack));
    disabled_stack.ss_flags = SS_DISABLE;
    sigaltstack(&disabled_stack, NULL);
    alternate_signal_stack_installed = false;
}

static bool prepare_alternate_signal_stack(void) {
    stack_t current_stack;
    if (sigaltstack(NULL, &current_stack) != 0) {
        return false;
    }
    if ((current_stack.ss_flags & SS_DISABLE) == 0) {
        return true;
    }

    stack_t new_stack;
    memset(&new_stack, 0, sizeof(new_stack));
    new_stack.ss_sp = alternate_signal_stack;
    new_stack.ss_size = sizeof(alternate_signal_stack);
    if (sigaltstack(&new_stack, NULL) != 0) {
        return false;
    }
    alternate_signal_stack_installed = true;
    return true;
}

static bool signal_will_reraise_autonomously(
    int signal_number,
    const siginfo_t *signal_info) {
    if (signal_info == NULL ||
        (signal_number != SIGBUS && signal_number != SIGFPE &&
         signal_number != SIGILL && signal_number != SIGSEGV)) {
        return false;
    }

    int signal_code = signal_info->si_code;
    return signal_code > 0 && signal_code != SI_ASYNCIO && signal_code != SI_MESGQ &&
        signal_code != SI_QUEUE && signal_code != SI_TIMER && signal_code != SI_USER &&
#ifdef SI_DETHREAD
        signal_code != SI_DETHREAD &&
#endif
#ifdef SI_KERNEL
        signal_code != SI_KERNEL &&
#endif
#ifdef SI_SIGIO
        signal_code != SI_SIGIO &&
#endif
#ifdef SI_TKILL
        signal_code != SI_TKILL &&
#endif
        true;
}

static void restore_previous_handler_and_reraise(
    int signal_number,
    siginfo_t *signal_info) {
    int index = find_signal_index(signal_number);
    if (index < 0) {
        return;
    }

    struct sigaction previous = previous_actions[index];
    // Re-deliver through the kernel so the previous action keeps its own mask and flags. Only the
    // crashing signal is restored; registrations for the other fatal signals remain untouched.
    if (sigaction(signal_number, &previous, NULL) != 0) {
        _exit(128 + signal_number);
    }
    if (previous.sa_handler == SIG_IGN) {
        return;
    }

#if defined(SYS_rt_tgsigqueueinfo) && defined(SYS_gettid)
    if (signal_info != NULL) {
        if (syscall(
                SYS_rt_tgsigqueueinfo,
                getpid(),
                syscall(SYS_gettid),
                signal_number,
                signal_info) == 0) {
            return;
        }
        // Linux kernels before 3.9 reject self-sent siginfo with EPERM. Other failures are
        // unexpected, so stop rather than invoke the previous handler with altered semantics.
        if (errno != EPERM) {
            _exit(128 + signal_number);
        }
    }
#endif

    // A synchronous hardware fault will recur when this handler returns. Raising it here would
    // replace the original fault details, including si_addr, on older kernels.
    if (!signal_will_reraise_autonomously(signal_number, signal_info) &&
        raise(signal_number) != 0) {
        _exit(128 + signal_number);
    }
}

static void handle_signal(
    int signal_number,
    siginfo_t *signal_info,
    void *user_context) {
    (void) user_context;
    int saved_errno = errno;
    if (!atomic_flag_test_and_set_explicit(&handling_signal, memory_order_relaxed)) {
        write_crash_marker(signal_number);
    }
    errno = saved_errno;
    restore_previous_handler_and_reraise(signal_number, signal_info);
    errno = saved_errno;
}

static bool install_handlers(void) {
    if (!prepare_alternate_signal_stack()) {
        return false;
    }

    struct sigaction action;
    memset(&action, 0, sizeof(action));
    if (sigfillset(&action.sa_mask) != 0) {
        remove_alternate_signal_stack();
        return false;
    }
    action.sa_sigaction = handle_signal;
    action.sa_flags = SA_RESTART | SA_SIGINFO | SA_ONSTACK | SA_RESETHAND | SA_EXPOSE_TAGBITS;

    for (int index = 0; index < SIGNAL_COUNT; index++) {
        if (!atomic_is_lock_free(&handler_active[index])) {
            remove_alternate_signal_stack();
            return false;
        }
        if (sigaction(handled_signals[index], NULL, &previous_actions[index]) != 0) {
            remove_alternate_signal_stack();
            return false;
        }
        atomic_store_explicit(&handler_active[index], false, memory_order_relaxed);
    }

    for (int index = 0; index < SIGNAL_COUNT; index++) {
        if (previous_actions[index].sa_handler == SIG_IGN) {
            continue;
        }
        atomic_store_explicit(&handler_active[index], true, memory_order_relaxed);
        if (sigaction(handled_signals[index], &action, NULL) != 0) {
            atomic_store_explicit(&handler_active[index], false, memory_order_relaxed);
            rollback_installed_handlers();
            remove_alternate_signal_stack();
            return false;
        }
    }
    return true;
}

static bool build_crash_record_paths(
    const char *path,
    size_t path_length,
    char *record_path,
    size_t record_path_capacity,
    char *temporary_record_path,
    size_t temporary_record_path_capacity) {
    size_t suffix_length = sizeof(TEMPORARY_SUFFIX) - 1;
    if (path == NULL || path_length == 0 || path_length >= record_path_capacity ||
        suffix_length >= temporary_record_path_capacity ||
        path_length >= temporary_record_path_capacity - suffix_length) {
        return false;
    }
    memcpy(record_path, path, path_length);
    record_path[path_length] = '\0';
    memcpy(temporary_record_path, path, path_length);
    memcpy(temporary_record_path + path_length, TEMPORARY_SUFFIX, suffix_length + 1);
    return true;
}

// Called under install_mutex. Unpublished state may be retried; published state lives until
// process exit and must never be prepared again. Future capture must acquire-load ready first.
static bool prepare_snapshot_and_install(
    struct snapshot_capture_state *state,
    const char *path,
    size_t path_length,
    otel_module_iterator iterate,
    bool (*install)(void)) {
    if (atomic_load_explicit(&state->ready, memory_order_acquire)) {
        return false;
    }
    int saved_errno = errno;
    memset(state->path, 0, sizeof(state->path));
    memset(state->temporary_path, 0, sizeof(state->temporary_path));
    memset(&state->snapshot, 0, sizeof(state->snapshot));
    bool prepared =
        OTEL_NCS_ARCH_CURRENT != OTEL_NCS_ARCH_UNKNOWN &&
        build_crash_record_paths(
            path, path_length, state->path, sizeof(state->path),
            state->temporary_path, sizeof(state->temporary_path));
    if (prepared) {
        state->snapshot.module_count =
            otel_native_crash_prepare_modules(state->snapshot.modules, iterate);
        prepared = state->snapshot.module_count != 0;
    }
    if (prepared) {
        static const unsigned char magic[] = OTEL_NCS_MAGIC;
        memcpy(state->snapshot.magic, magic, sizeof(magic));
        state->snapshot.version = OTEL_NCS_VERSION;
        state->snapshot.architecture = OTEL_NCS_ARCH_CURRENT;
        state->snapshot.record_size = sizeof(state->snapshot);
    }
    errno = saved_errno;
    bool installed = install();
    // A signal arriving during installation must use marker-only capture. Do not publish
    // partial installation state: an unsuccessful attempt can be retried with another path.
    if (installed && prepared) {
        atomic_store_explicit(&state->ready, true, memory_order_release);
    }
    return installed;
}

static bool install_for_path(
    const char *path, size_t path_length,
    const char *snapshot_path, size_t snapshot_path_length) {
    pthread_mutex_lock(&install_mutex);
    bool installed;
    if (handlers_installed) {
        installed = strcmp(crash_record_path, path) == 0 &&
            (!atomic_load_explicit(&snapshot_capture.ready, memory_order_acquire) ||
                (snapshot_path != NULL && strcmp(snapshot_capture.path, snapshot_path) == 0));
    } else {
        installed =
            build_crash_record_paths(
                path,
                path_length,
                crash_record_path,
                sizeof(crash_record_path),
                temporary_crash_record_path,
                sizeof(temporary_crash_record_path)) &&
            prepare_snapshot_and_install(
                &snapshot_capture, snapshot_path, snapshot_path_length, dl_iterate_phdr,
                install_handlers);
        handlers_installed = installed;
    }
    pthread_mutex_unlock(&install_mutex);
    return installed;
}

JNIEXPORT jboolean JNICALL
Java_io_opentelemetry_android_instrumentation_nativecrash_NativeCrashJni_installForPaths(
    JNIEnv *environment,
    jclass native_crash_jni,
    jstring marker_path,
    jstring snapshot_path) {
    (void) native_crash_jni;
    if (marker_path == NULL) {
        return JNI_FALSE;
    }

    jsize path_length = (*environment)->GetStringUTFLength(environment, marker_path);
    if (path_length <= 0) {
        return JNI_FALSE;
    }

    const char *path = (*environment)->GetStringUTFChars(environment, marker_path, NULL);
    if (path == NULL) {
        return JNI_FALSE;
    }

    const char *snapshot = NULL;
    jsize snapshot_length = 0;
    if (snapshot_path != NULL) {
        snapshot_length = (*environment)->GetStringUTFLength(environment, snapshot_path);
        snapshot = (*environment)->GetStringUTFChars(environment, snapshot_path, NULL);
        if (snapshot == NULL) {
            (*environment)->ReleaseStringUTFChars(environment, marker_path, path);
            return JNI_FALSE;
        }
    }
    bool installed =
        install_for_path(path, (size_t) path_length, snapshot, (size_t) snapshot_length);

    if (snapshot != NULL) {
        (*environment)->ReleaseStringUTFChars(environment, snapshot_path, snapshot);
    }
    (*environment)->ReleaseStringUTFChars(environment, marker_path, path);
    return installed ? JNI_TRUE : JNI_FALSE;
}

#ifdef OTEL_NATIVE_CRASH_TESTING
static struct snapshot_capture_state *test_preparing_state;
static unsigned int test_install_calls;

static bool test_install_success(void) {
    test_install_calls++;
    return !atomic_load_explicit(&test_preparing_state->ready, memory_order_acquire) &&
        errno == EDOM;
}

static bool test_install_failure(void) {
    test_install_calls++;
    return false;
}

static int test_no_modules(int (*callback)(struct dl_phdr_info *, size_t, void *), void *data) {
    (void) callback;
    (void) data;
    errno = EIO;
    return -1;
}

JNIEXPORT jboolean JNICALL
Java_io_opentelemetry_android_instrumentation_nativecrash_NativeCrashPreparationTestJni_check(
    JNIEnv *environment, jclass clazz, jint scenario) {
    (void) environment;
    (void) clazz;
    // These hooks run serially and never install signal handlers or mutate production state.
    struct snapshot_capture_state state = {0};
    test_preparing_state = &state;
    test_install_calls = 0;
    errno = EDOM;
    static const char path[] = "/test/native-crash-snapshot.bin";
    bool passed = false;
    if (scenario == 0) {
        bool installed = prepare_snapshot_and_install(
            &state, path, sizeof(path) - 1, dl_iterate_phdr, test_install_success);
        struct otel_native_crash_snapshot expected = {
            .magic = OTEL_NCS_MAGIC, .version = OTEL_NCS_VERSION,
            .architecture = OTEL_NCS_ARCH_CURRENT, .record_size = sizeof(expected),
            .module_count = state.snapshot.module_count,
        };
        memcpy(expected.modules, state.snapshot.modules, sizeof(expected.modules));
        passed = installed && atomic_load(&state.ready) && test_install_calls == 1 &&
            state.snapshot.module_count > 0 && memcmp(&expected, &state.snapshot, sizeof(expected)) == 0 &&
            strcmp(state.path, path) == 0 &&
            strcmp(state.temporary_path, "/test/native-crash-snapshot.bin.tmp") == 0;
    } else if (scenario == 1) {
        bool failed = !prepare_snapshot_and_install(
            &state, path, sizeof(path) - 1, dl_iterate_phdr, test_install_failure);
        bool unpublished = !atomic_load(&state.ready);
        state.snapshot.stack[0] = 0xa5;
        passed = failed && unpublished && prepare_snapshot_and_install(
            &state, "/retry", 6, dl_iterate_phdr, test_install_success) &&
            atomic_load(&state.ready) && test_install_calls == 2 &&
            strcmp(state.path, "/retry") == 0 && state.snapshot.stack[0] == 0;
    } else if (scenario == 2) {
        passed = prepare_snapshot_and_install(
            &state, path, sizeof(path) - 1, test_no_modules, test_install_success) &&
            !atomic_load(&state.ready) && state.snapshot.module_count == 0 &&
            test_install_calls == 1;
    } else if (scenario == 3) {
        char too_long[PATH_MAX];
        memset(too_long, 'a', sizeof(too_long));
        passed = prepare_snapshot_and_install(
            &state, too_long, sizeof(too_long) - 4, dl_iterate_phdr, test_install_success) &&
            !atomic_load(&state.ready) && state.path[0] == '\0' &&
            prepare_snapshot_and_install(&state, NULL, 0, dl_iterate_phdr, test_install_success) &&
            !atomic_load(&state.ready) &&
            prepare_snapshot_and_install(&state, "", 0, dl_iterate_phdr, test_install_success) &&
            !atomic_load(&state.ready) && test_install_calls == 3;
    } else if (scenario == 4) {
        char longest[PATH_MAX - 4];
        memset(longest, 'a', sizeof(longest));
        longest[sizeof(longest) - 1] = '\0';
        passed = prepare_snapshot_and_install(
            &state, longest, strlen(longest), dl_iterate_phdr, test_install_success) &&
            atomic_load(&state.ready) && strlen(state.temporary_path) == PATH_MAX - 1 &&
            strcmp(state.temporary_path + strlen(longest), ".tmp") == 0;
    } else if (scenario == 5) {
        bool installed = prepare_snapshot_and_install(
            &state, path, sizeof(path) - 1, dl_iterate_phdr, test_install_success);
        struct otel_native_crash_snapshot before = state.snapshot;
        passed = installed &&
            !prepare_snapshot_and_install(&state, "/other", 6, test_no_modules, test_install_failure) &&
            atomic_load(&state.ready) && test_install_calls == 1 &&
            strcmp(state.path, path) == 0 && memcmp(&before, &state.snapshot, sizeof(before)) == 0;
    }
    test_preparing_state = NULL;
    return passed ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL
Java_io_opentelemetry_android_instrumentation_nativecrash_NativeCrashPreparationTestJni_preparedSnapshot(
    JNIEnv *environment, jclass clazz) {
    (void) clazz;
    if (!atomic_load_explicit(&snapshot_capture.ready, memory_order_acquire)) return NULL;
    jbyteArray result = (*environment)->NewByteArray(environment, sizeof(snapshot_capture.snapshot));
    if (result != NULL) {
        (*environment)->SetByteArrayRegion(
            environment, result, 0, sizeof(snapshot_capture.snapshot),
            (const jbyte *) &snapshot_capture.snapshot);
    }
    return result;
}

JNIEXPORT jboolean JNICALL
Java_io_opentelemetry_android_instrumentation_nativecrash_NativeCrashTestJni_writeCrashMarker(
    JNIEnv *environment,
    jclass native_crash_test_jni,
    jstring marker_path,
    jint signal_number,
    jlong timestamp_epoch_nanos) {
    (void) native_crash_test_jni;
    if (marker_path == NULL || signal_number <= 0 || timestamp_epoch_nanos <= 0) {
        return JNI_FALSE;
    }

    jsize path_length = (*environment)->GetStringUTFLength(environment, marker_path);
    if (path_length <= 0) {
        return JNI_FALSE;
    }

    const char *path = (*environment)->GetStringUTFChars(environment, marker_path, NULL);
    if (path == NULL) {
        return JNI_FALSE;
    }

    char test_crash_record_path[PATH_MAX];
    char test_temporary_crash_record_path[PATH_MAX];
    bool written =
        build_crash_record_paths(
            path,
            (size_t) path_length,
            test_crash_record_path,
            sizeof(test_crash_record_path),
            test_temporary_crash_record_path,
            sizeof(test_temporary_crash_record_path)) &&
        write_crash_marker_at(
            test_crash_record_path,
            test_temporary_crash_record_path,
            (int) signal_number,
            (uint64_t) timestamp_epoch_nanos);

    (*environment)->ReleaseStringUTFChars(environment, marker_path, path);
    return written ? JNI_TRUE : JNI_FALSE;
}
#endif
