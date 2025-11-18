plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android ANR instrumentation common-api"

android {
    namespace = "io.opentelemetry.android.instrumentation.common"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":agent-api"))
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
    testImplementation(libs.androidx.fragment)
}
