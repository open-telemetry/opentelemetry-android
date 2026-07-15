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
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#define SIGNAL_COUNT 6
#define TEMPORARY_SUFFIX ".tmp"
#define MARKER_BUFFER_SIZE 128
#define NANOS_PER_SECOND UINT64_C(1000000000)

static const int handled_signals[SIGNAL_COUNT] = {
    SIGILL,
    SIGTRAP,
    SIGABRT,
    SIGBUS,
    SIGFPE,
    SIGSEGV,
};

static char crash_record_path[PATH_MAX];
static char temporary_crash_record_path[PATH_MAX];
static struct sigaction previous_actions[SIGNAL_COUNT];
static pthread_mutex_t install_mutex = PTHREAD_MUTEX_INITIALIZER;
static bool handlers_installed = false;
static volatile sig_atomic_t handling_signal = 0;

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

static void restore_previous_handlers(void) {
    for (int index = 0; index < SIGNAL_COUNT; index++) {
        sigaction(handled_signals[index], &previous_actions[index], NULL);
    }
}

static void invoke_previous_handler(
    int signal_number,
    siginfo_t *signal_info,
    void *user_context) {
    int index = find_signal_index(signal_number);
    restore_previous_handlers();
    if (index < 0) {
        return;
    }

    struct sigaction previous = previous_actions[index];
    if (previous.sa_handler == SIG_DFL) {
        raise(signal_number);
    } else if (previous.sa_handler == SIG_IGN) {
        return;
    } else if ((previous.sa_flags & SA_SIGINFO) != 0) {
        previous.sa_sigaction(signal_number, signal_info, user_context);
    } else {
        previous.sa_handler(signal_number);
    }
}

static void handle_signal(
    int signal_number,
    siginfo_t *signal_info,
    void *user_context) {
    int saved_errno = errno;
    if (!handling_signal) {
        handling_signal = 1;
        write_crash_marker(signal_number);
    }
    invoke_previous_handler(signal_number, signal_info, user_context);
    errno = saved_errno;
}

static bool install_handlers(void) {
    struct sigaction action;
    memset(&action, 0, sizeof(action));
    sigemptyset(&action.sa_mask);
    for (int index = 0; index < SIGNAL_COUNT; index++) {
        sigaddset(&action.sa_mask, handled_signals[index]);
    }
    action.sa_sigaction = handle_signal;
    action.sa_flags = SA_SIGINFO | SA_ONSTACK | SA_RESETHAND;

    int installed_count = 0;
    for (; installed_count < SIGNAL_COUNT; installed_count++) {
        if (sigaction(
                handled_signals[installed_count],
                &action,
                &previous_actions[installed_count]) != 0) {
            break;
        }
    }
    if (installed_count == SIGNAL_COUNT) {
        return true;
    }
    while (installed_count > 0) {
        installed_count--;
        sigaction(
            handled_signals[installed_count],
            &previous_actions[installed_count],
            NULL);
    }
    return false;
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
