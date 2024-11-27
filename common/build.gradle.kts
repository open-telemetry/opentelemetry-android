plugins {
    id("otel.android-library-conventions")
}

description = "OpenTelemetry android common utils"

android {
    namespace = "io.opentelemetry.android.common"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
    implementation(libs.opentelemetry.semconv.incubating)
    implementation(libs.androidx.core)

    testImplementation(libs.robolectric)
}
