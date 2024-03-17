plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android ANR instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.lifecycle"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform))
    api(project(":instrumentation:activity")) // TODO: Should lifecycle/fragment/activity share a root?
    api(project(":instrumentation:common-api"))
    api(project(":instrumentation:fragment")) // TODO: Should lifecycle/fragment/activity share a root?
    api(libs.androidx.navigation.fragment)
    api(libs.opentelemetry.api)
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.semconv)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
}
