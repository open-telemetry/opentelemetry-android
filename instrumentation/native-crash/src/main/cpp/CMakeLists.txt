# Copyright The OpenTelemetry Authors
# SPDX-License-Identifier: Apache-2.0

cmake_minimum_required(VERSION 3.22.1)
project(otel_android_native_crash LANGUAGES C)

option(OTEL_NATIVE_CRASH_TESTING "Build native crash instrumentation test hooks" OFF)

add_library(otel_android_native_crash SHARED native_crash_handler.c native_crash_modules.c)

set_target_properties(
    otel_android_native_crash
    PROPERTIES
        C_STANDARD 11
        C_STANDARD_REQUIRED YES
        C_EXTENSIONS YES
)

target_compile_options(
    otel_android_native_crash
    PRIVATE
        -Wall
        -Wextra
        -Werror
)

if(OTEL_NATIVE_CRASH_TESTING)
    target_sources(
        otel_android_native_crash PRIVATE ../../androidTest/cpp/native_crash_modules_test.c
    )
    target_link_libraries(otel_android_native_crash PRIVATE dl)
    target_compile_definitions(
        otel_android_native_crash
        PRIVATE
            OTEL_NATIVE_CRASH_TESTING=1
    )
endif()
