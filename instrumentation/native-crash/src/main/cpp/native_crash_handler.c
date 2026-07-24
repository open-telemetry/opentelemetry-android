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

static void write_crash_marker(int signal_number) {
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
    uint64_t timestamp_epoch_nanos = seconds * NANOS_PER_SECOND + nanoseconds;

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
        return;
    }

    int file_descriptor =
        open(temporary_crash_record_path, O_CREAT | O_WRONLY | O_TRUNC | O_CLOEXEC, 0600);
    if (file_descriptor < 0) {
        return;
    }

    bool complete = write_all(file_descriptor, marker, marker_length) && fsync(file_descriptor) == 0;
    if (close(file_descriptor) != 0) {
        complete = false;
    }
    if (complete) {
        if (rename(temporary_crash_record_path, crash_record_path) != 0) {
            unlink(temporary_crash_record_path);
        }
    } else {
        unlink(temporary_crash_record_path);
    }
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

static bool install_for_path(const char *path, size_t path_length) {
    size_t suffix_length = sizeof(TEMPORARY_SUFFIX) - 1;
    if (path == NULL || path_length == 0 || path_length >= sizeof(crash_record_path) ||
        path_length + suffix_length >= sizeof(temporary_crash_record_path)) {
        return false;
    }

    pthread_mutex_lock(&install_mutex);
    bool installed;
    if (handlers_installed) {
        installed = strcmp(crash_record_path, path) == 0;
    } else {
        memcpy(crash_record_path, path, path_length);
        crash_record_path[path_length] = '\0';
        memcpy(temporary_crash_record_path, path, path_length);
        memcpy(temporary_crash_record_path + path_length, TEMPORARY_SUFFIX, suffix_length + 1);
        installed = install_handlers();
        handlers_installed = installed;
    }
    pthread_mutex_unlock(&install_mutex);
    return installed;
}

JNIEXPORT jboolean JNICALL
Java_io_opentelemetry_android_instrumentation_nativecrash_NativeCrashJni_install(
    JNIEnv *environment,
    jclass native_crash_jni,
    jstring marker_path) {
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

    bool installed = install_for_path(path, (size_t) path_length);

    (*environment)->ReleaseStringUTFChars(environment, marker_path, path);
    return installed ? JNI_TRUE : JNI_FALSE;
}
