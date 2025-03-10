plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry build-time auto-instrumentation for OkHttp Websocket on Android"

android {
    namespace = "io.opentelemetry.android.okhttp.websocket.agent"
}

dependencies {
    implementation(project(":instrumentation:okhttp:websocket:okhttp-3.0:library"))
    compileOnly(libs.okhttp)
    implementation(libs.byteBuddy)
}
