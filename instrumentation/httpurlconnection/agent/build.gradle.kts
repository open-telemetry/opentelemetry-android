plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry build-time auto-instrumentation for HttpURLConnection on Android"

otelAndroid.minSdk = 21

dependencies {
    implementation(project(":instrumentation:httpurlconnection:library"))
    implementation(libs.byteBuddy)
}
