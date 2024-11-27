plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android ANR instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.anr"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(project(":instrumentation:android-instrumentation"))
    implementation(project(":services"))
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.semconv)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
}
