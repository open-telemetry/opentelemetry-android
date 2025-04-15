plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry build-time auto-instrumentation for OkHttp on Android"

android {
    namespace = "io.opentelemetry.android.okhttp.agent"
}

dependencies {
    implementation(libs.okhttp)
    implementation(project(":instrumentation:okhttp3:library"))
    implementation(libs.byteBuddy)
}
