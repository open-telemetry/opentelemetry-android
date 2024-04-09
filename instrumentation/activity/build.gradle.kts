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
    api(platform(libs.opentelemetry.platform))
    api(libs.opentelemetry.api)
    api(project(":android-agent"))
    api(project(":instrumentation:common-api"))
    api(project(":common"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.opentelemetry.instrumentation.api)
}
