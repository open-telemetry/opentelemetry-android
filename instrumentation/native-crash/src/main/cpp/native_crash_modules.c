/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

#include "native_crash_modules.h"

#include <errno.h>
#include <stdbool.h>
#include <string.h>

struct module_collection {
    struct otel_native_crash_module *modules;
    uint32_t count;
    uint32_t app_count;
};

// Match the snapshot reader's strict UTF-8 and no-control-character name contract.
static bool valid_name(const char *name, size_t length) {
    bool nonblank = false;
    for (size_t offset = 0; offset < length;) {
        uint32_t value = (unsigned char) name[offset++];
        uint32_t minimum = 0;
        unsigned int remaining = 0;
        if (value >= 0xc2 && value <= 0xdf) {
            remaining = 1;
            minimum = 0x80;
            value &= 0x1f;
        } else if (value >= 0xe0 && value <= 0xef) {
            remaining = 2;
            minimum = 0x800;
            value &= 0x0f;
        } else if (value >= 0xf0 && value <= 0xf4) {
            remaining = 3;
            minimum = 0x10000;
            value &= 0x07;
        } else if (value >= 0x80) {
            return false;
        }
        if (remaining > length - offset) return false;
        while (remaining > 0) {
            remaining--;
            unsigned char next = (unsigned char) name[offset++];
            if ((next & 0xc0) != 0x80) return false;
            value = (value << 6) | (next & 0x3f);
        }
        if (value < minimum || value > 0x10ffff || (value >= 0xd800 && value <= 0xdfff) ||
            value < 0x20 || (value >= 0x7f && value <= 0x9f)) return false;
        bool whitespace = value == 0x20 || value == 0xa0 || value == 0x1680 ||
            (value >= 0x2000 && value <= 0x200a) || value == 0x2028 || value == 0x2029 ||
            value == 0x202f || value == 0x205f || value == 0x3000;
        nonblank |= !whitespace;
    }
    return nonblank;
}

static int collect_module(struct dl_phdr_info *info, size_t size, void *data) {
    if (info == NULL || size < offsetof(struct dl_phdr_info, dlpi_phnum) + sizeof(info->dlpi_phnum) ||
        info->dlpi_name == NULL || info->dlpi_phdr == NULL) return 0;
    const char *name = strrchr(info->dlpi_name, '/');
    name = name == NULL ? info->dlpi_name : name + 1;
    size_t length = strnlen(name, OTEL_NCS_MODULE_NAME_SIZE);
    // Reject long names rather than truncate an artifact name or a UTF-8 code point.
    if (length == OTEL_NCS_MODULE_NAME_SIZE || !valid_name(name, length)) return 0;

    bool app_owned = strncmp(info->dlpi_name, "/data/", 6) == 0 ||
        strncmp(info->dlpi_name, "/mnt/expand/", 12) == 0;
    struct module_collection *collection = data;
    for (size_t index = 0; index < info->dlpi_phnum; index++) {
        const ElfW(Phdr) *header = &info->dlpi_phdr[index];
        if (header->p_type != PT_LOAD || (header->p_flags & PF_X) == 0 || header->p_memsz == 0 ||
            header->p_vaddr > UINTPTR_MAX - info->dlpi_addr) continue;
        uintptr_t start = info->dlpi_addr + header->p_vaddr;
        if (header->p_memsz > UINTPTR_MAX - start) continue;

        uint32_t position = app_owned ? collection->app_count : collection->count;
        if (position == OTEL_NCS_MAX_MODULES) continue;
        if (collection->count < OTEL_NCS_MAX_MODULES) collection->count++;
        // Keep loader order within each group, evicting the last non-app entry if full.
        memmove(&collection->modules[position + 1], &collection->modules[position],
                (collection->count - position - 1) * sizeof(*collection->modules));
        struct otel_native_crash_module *module = &collection->modules[position];
        memset(module, 0, sizeof(*module));
        module->load_bias = info->dlpi_addr;
        module->executable_start = start;
        module->executable_end = start + header->p_memsz;
        memcpy(module->name, name, length);
        if (app_owned) collection->app_count++;
    }
    return 0;
}

uint32_t otel_native_crash_prepare_modules(
    struct otel_native_crash_module modules[OTEL_NCS_MAX_MODULES], otel_module_iterator iterate) {
    if (modules == NULL) return 0;
    int saved_errno = errno;
    size_t capacity = sizeof(*modules) * OTEL_NCS_MAX_MODULES;
    memset(modules, 0, capacity);
    struct module_collection collection = {.modules = modules};
    if (iterate == NULL || iterate(collect_module, &collection) != 0) {
        memset(modules, 0, capacity);
        collection.count = 0;
    }
    errno = saved_errno;
    return collection.count;
}
