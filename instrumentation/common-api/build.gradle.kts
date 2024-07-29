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
    api(project(":core"))
    api(platform(libs.opentelemetry.platform))
    api(libs.opentelemetry.api)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
}
