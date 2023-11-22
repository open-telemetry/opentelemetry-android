plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry HttpURLConnection agent instrumentation for Android"

otelAndroid.minSdk = 21

dependencies {
    implementation(project(":auto-instrumentation:httpurlconnection:library"))
    implementation(libs.byteBuddy)
}
