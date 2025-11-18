plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android Logging instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.log"
}

dependencies {
    implementation(project(":agent-api"))

    implementation(libs.androidx.core)
    implementation(libs.byteBuddy)
    implementation(project(":instrumentation:android-log:library"))
}
