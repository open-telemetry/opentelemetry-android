plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry build-time auto-instrumentation for HttpURLConnection on Android"

android {
    namespace = "io.opentelemetry.android.httpurlconnection.agent"
}

dependencies {
    implementation(project(":instrumentation:httpurlconnection:library"))
    implementation(libs.byteBuddy)
}
