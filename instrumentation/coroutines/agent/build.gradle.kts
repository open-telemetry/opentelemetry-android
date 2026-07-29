/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android coroutines context propagation agent"

android {
    namespace = "io.opentelemetry.android.instrumentation.coroutines"
}

dependencies {
    implementation(project(":instrumentation:coroutines:library"))
    implementation(libs.byteBuddy)
}
