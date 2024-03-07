plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry build-time auto-instrumentation for OkHttp on Android"

android {
    namespace = "io.opentelemetry.android.okhttp.agent"
}

dependencies {
    implementation(project(":instrumentation:okhttp:okhttp-3.0:library"))
    implementation(libs.okhttp)
    implementation(libs.byteBuddy)
}
