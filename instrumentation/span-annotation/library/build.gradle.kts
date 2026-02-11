plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "placeholder"

android {
    namespace = "io.opentelemetry.android.spanannotation.library"
}

dependencies {
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.context)
    implementation(libs.opentelemetry.instrumentation.annotations)
    api(project(":instrumentation:android-instrumentation"))
}
