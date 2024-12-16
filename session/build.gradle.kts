plugins {
    id("otel.android-library-conventions")
}

description = "OpenTelemetry android session api"

android {
    namespace = "io.opentelemetry.android.session"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)
    implementation(project(":services"))
}
