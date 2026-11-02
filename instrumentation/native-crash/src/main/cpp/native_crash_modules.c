/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

#include "native_crash_modules.h"

#include <errno.h>
#include <stdbool.h>
#include <string.h>

#define UTF8_ASCII_LIMIT 0x80
#define UTF8_TWO_BYTE_LEAD_MIN 0xc2
#define UTF8_TWO_BYTE_LEAD_MAX 0xdf
#define UTF8_TWO_BYTE_CODE_POINT_MIN 0x80
#define UTF8_TWO_BYTE_PAYLOAD_MASK 0x1f
#define UTF8_THREE_BYTE_LEAD_MIN 0xe0
#define UTF8_THREE_BYTE_LEAD_MAX 0xef
#define UTF8_THREE_BYTE_CODE_POINT_MIN 0x800
#define UTF8_THREE_BYTE_PAYLOAD_MASK 0x0f
#define UTF8_FOUR_BYTE_LEAD_MIN 0xf0
#define UTF8_FOUR_BYTE_LEAD_MAX 0xf4
#define UTF8_FOUR_BYTE_CODE_POINT_MIN 0x10000
#define UTF8_FOUR_BYTE_PAYLOAD_MASK 0x07
#define UTF8_CONTINUATION_PREFIX_MASK 0xc0
#define UTF8_CONTINUATION_PREFIX 0x80
#define UTF8_CONTINUATION_PAYLOAD_MASK 0x3f
#define UTF8_CONTINUATION_PAYLOAD_BITS 6

#define UNICODE_CODE_POINT_MAX 0x10ffff
#define UNICODE_SURROGATE_FIRST 0xd800
#define UNICODE_SURROGATE_LAST 0xdfff
#define UNICODE_C0_CONTROL_LIMIT 0x20
#define UNICODE_DELETE 0x7f
#define UNICODE_C1_CONTROL_LAST 0x9f

#define UNICODE_SPACE 0x20
#define UNICODE_NO_BREAK_SPACE 0xa0
#define UNICODE_OGHAM_SPACE_MARK 0x1680
#define UNICODE_EN_QUAD 0x2000
#define UNICODE_HAIR_SPACE 0x200a
#define UNICODE_LINE_SEPARATOR 0x2028
#define UNICODE_PARAGRAPH_SEPARATOR 0x2029
#define UNICODE_NARROW_NO_BREAK_SPACE 0x202f
#define UNICODE_MEDIUM_MATHEMATICAL_SPACE 0x205f
#define UNICODE_IDEOGRAPHIC_SPACE 0x3000

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
        if (value >= UTF8_TWO_BYTE_LEAD_MIN && value <= UTF8_TWO_BYTE_LEAD_MAX) {
            remaining = 1;
            minimum = UTF8_TWO_BYTE_CODE_POINT_MIN;
            value &= UTF8_TWO_BYTE_PAYLOAD_MASK;
        } else if (value >= UTF8_THREE_BYTE_LEAD_MIN && value <= UTF8_THREE_BYTE_LEAD_MAX) {
            remaining = 2;
            minimum = UTF8_THREE_BYTE_CODE_POINT_MIN;
            value &= UTF8_THREE_BYTE_PAYLOAD_MASK;
        } else if (value >= UTF8_FOUR_BYTE_LEAD_MIN && value <= UTF8_FOUR_BYTE_LEAD_MAX) {
            remaining = 3;
            minimum = UTF8_FOUR_BYTE_CODE_POINT_MIN;
            value &= UTF8_FOUR_BYTE_PAYLOAD_MASK;
        } else if (value >= UTF8_ASCII_LIMIT) {
            return false;
        }
        if (remaining > length - offset) return false;
        while (remaining > 0) {
            remaining--;
            unsigned char next = (unsigned char) name[offset++];
            if ((next & UTF8_CONTINUATION_PREFIX_MASK) != UTF8_CONTINUATION_PREFIX) return false;
            value = (value << UTF8_CONTINUATION_PAYLOAD_BITS) | (next & UTF8_CONTINUATION_PAYLOAD_MASK);
        }
        if (value < minimum || value > UNICODE_CODE_POINT_MAX ||
            (value >= UNICODE_SURROGATE_FIRST && value <= UNICODE_SURROGATE_LAST) ||
            value < UNICODE_C0_CONTROL_LIMIT ||
            (value >= UNICODE_DELETE && value <= UNICODE_C1_CONTROL_LAST)) return false;
        bool whitespace = value == UNICODE_SPACE || value == UNICODE_NO_BREAK_SPACE ||
            value == UNICODE_OGHAM_SPACE_MARK ||
            (value >= UNICODE_EN_QUAD && value <= UNICODE_HAIR_SPACE) ||
            value == UNICODE_LINE_SEPARATOR || value == UNICODE_PARAGRAPH_SEPARATOR ||
            value == UNICODE_NARROW_NO_BREAK_SPACE || value == UNICODE_MEDIUM_MATHEMATICAL_SPACE ||
            value == UNICODE_IDEOGRAPHIC_SPACE;
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
