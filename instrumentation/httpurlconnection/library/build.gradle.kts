plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry HttpURLConnection library instrumentation for Android"

android {
    namespace = "io.opentelemetry.android.httpurlconnection.library"
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":agent-api"))
    api(libs.opentelemetry.context)
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.instrumentation.api)
}
