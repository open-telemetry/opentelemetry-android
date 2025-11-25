plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android Log library instrumentation for Android"

android {
    namespace = "io.opentelemetry.android.log.library"
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":agent-api"))

    implementation(libs.opentelemetry.instrumentation.apiSemconv)
}
