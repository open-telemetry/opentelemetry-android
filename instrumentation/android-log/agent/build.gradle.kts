plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android Logging instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.log"
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)

    implementation(libs.androidx.core)
    implementation(libs.byteBuddy)
    implementation(project(":instrumentation:android-log:library"))
}
