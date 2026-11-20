/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef OTEL_ANDROID_NATIVE_CRASH_MODULES_H
#define OTEL_ANDROID_NATIVE_CRASH_MODULES_H

#include <link.h>
#include "native_crash_snapshot.h"

typedef int (*otel_module_iterator)(int (*callback)(struct dl_phdr_info *, size_t, void *), void *);

// Use dl_iterate_phdr in normal runtime, never in a signal handler. The caller owns the output
// and must finish preparation before publishing it; do not overwrite a table still in use.
// Returns the populated count, or zero with an empty table when enumeration fails.
// A zero result cannot be published as a snapshot; the format requires at least one module.
__attribute__((visibility("hidden")))
uint32_t otel_native_crash_prepare_modules(
    struct otel_native_crash_module modules[OTEL_NCS_MAX_MODULES], otel_module_iterator iterate);

#endif
