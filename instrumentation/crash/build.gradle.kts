plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android crash instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.crash"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    api(project(":core"))
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.semconv.incubating)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
    testImplementation(libs.awaitility)
    testImplementation(libs.robolectric)
}
