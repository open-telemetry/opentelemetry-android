plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android activity instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.activity"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":instrumentation:common-api"))
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":services"))
    implementation(project(":session"))
    implementation(project(":common"))
    implementation(libs.opentelemetry.sdk)
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.instrumentation.api)
    testImplementation(libs.robolectric)
}
