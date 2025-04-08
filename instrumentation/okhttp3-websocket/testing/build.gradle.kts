plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

android {
    namespace = "io.opentelemetry.android.okhttp.websocket.testing"
}

dependencies {
    implementation(project(":test-common"))
    byteBuddy(project(":instrumentation:okhttp3-websocket:agent"))
    implementation(project(":instrumentation:okhttp3-websocket:library"))

    implementation(libs.okhttp)
    implementation(libs.opentelemetry.exporter.otlp)
    androidTestImplementation(libs.okhttp.mockwebserver)
}
