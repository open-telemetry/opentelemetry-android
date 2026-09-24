/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

android {
    namespace = "io.opentelemetry.android.coroutines.test"
}

dependencies {
    byteBuddy(project(":instrumentation:coroutines:agent"))
    implementation(project(":instrumentation:coroutines:library"))
    implementation(project(":test-common"))
    implementation(libs.kotlinx.coroutines)

    androidTestImplementation(libs.assertj.core)
}
