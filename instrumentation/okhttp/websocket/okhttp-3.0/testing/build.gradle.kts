plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

android {
    namespace = "io.opentelemetry.android.okhttp.websocket.testing"
}

dependencies {
    implementation(project(":test-common"))
    byteBuddy(project(":instrumentation:okhttp:websocket:okhttp-3.0:agent"))
    implementation(project(":instrumentation:okhttp:websocket:okhttp-3.0:library"))

    implementation(libs.okhttp)
    implementation(libs.opentelemetry.exporter.otlp)
    androidTestImplementation(libs.okhttp.mockwebserver)
}
