plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry HttpURLConnection library instrumentation for Android"

android {
    namespace = "io.opentelemetry.android.httpurlconnection.library"
}

dependencies {
    api(project(":instrumentation:android-instrumentation"))
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    api(libs.opentelemetry.context)
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.instrumentation.api)
}
