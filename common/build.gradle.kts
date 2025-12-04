plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry android common utils"

android {
    namespace = "io.opentelemetry.android.common"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":agent-api"))
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
    implementation(libs.opentelemetry.semconv.incubating)
    implementation(libs.androidx.core)

    testImplementation(libs.robolectric)
}
