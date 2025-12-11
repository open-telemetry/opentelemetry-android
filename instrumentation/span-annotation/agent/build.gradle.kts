plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "placeholder"

android {
    namespace = "io.opentelemetry.android.spanannotation.agent"
}

dependencies {
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.context)
    implementation(libs.opentelemetry.instrumentation.annotations)
    implementation(project(":instrumentation:span-annotation:library"))
    implementation(libs.byteBuddy)
}
