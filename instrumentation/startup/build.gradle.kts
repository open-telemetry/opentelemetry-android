plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android startup instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.startup"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    api(project(":core"))
    api(project(":instrumentation:android-instrumentation"))
    implementation(project(":common"))
    implementation(project(":services"))
    implementation(project(":session"))
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.semconv)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.api.incubator)
    implementation(libs.opentelemetry.sdk.extension.incubator)
    implementation(libs.opentelemetry.instrumentation.api)
}
