plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android session instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.sessions"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    implementation(libs.opentelemetry.api.incubator)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.semconv.incubating)
    api(project(":instrumentation:common-api"))
    api(project(":core"))
}
