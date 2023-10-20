plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry build-time auto-instrumentation for OkHttp on Android"

dependencies {
    implementation(project(":auto-instrumentation:okhttp:okhttp-3.0:library"))
    implementation(libs.okhttp)
    implementation(libs.byteBuddy)
}
