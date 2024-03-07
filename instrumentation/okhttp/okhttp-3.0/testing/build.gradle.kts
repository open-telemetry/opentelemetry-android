plugins {
    id("otel.android-app-conventions")
    id("net.bytebuddy.byte-buddy-gradle-plugin")
}

dependencies {
    byteBuddy(project(":instrumentation:okhttp:okhttp-3.0:agent"))
    implementation(project(":instrumentation:okhttp:okhttp-3.0:library"))
    implementation(libs.okhttp)
    implementation(libs.opentelemetry.exporter.otlp)
    androidTestImplementation(libs.okhttp.mockwebserver)
}
