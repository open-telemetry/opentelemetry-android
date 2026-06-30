plugins {
    id("otel.android-library-conventions")
}

description = "OpenTelemetry Android View Gesture common utils"

android {
    namespace = "io.opentelemetry.android.instrumentation.view.common"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":agent-api"))
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
    testImplementation(libs.androidx.fragment)
}
