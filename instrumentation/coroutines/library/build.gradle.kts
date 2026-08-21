/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android coroutines context propagation library"

android {
    namespace = "io.opentelemetry.android.coroutines.library"
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(libs.opentelemetry.extension.kotlin)
    compileOnly(libs.kotlinx.coroutines)
    testImplementation(libs.kotlinx.coroutines)
}
