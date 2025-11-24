plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry android session api"

android {
    namespace = "io.opentelemetry.android.session"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":agent-api"))
    implementation(libs.opentelemetry.sdk)
}
