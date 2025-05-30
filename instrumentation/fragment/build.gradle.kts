plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android fragment instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.fragment"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    api(project(":instrumentation:common-api"))
    api(project(":instrumentation:android-instrumentation"))
    implementation(project(":services"))
    implementation(project(":common"))
    implementation(libs.androidx.core)
    api(libs.androidx.navigation.fragment)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
}
