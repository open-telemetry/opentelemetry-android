plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android slow rendering instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.slowrendering"

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
    implementation(project(":session"))
    implementation(project(":common"))
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.semconv)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.kotlin)
    implementation(libs.opentelemetry.kotlin.compat)
    implementation(libs.opentelemetry.instrumentation.api)
    implementation(libs.opentelemetry.sdk.extension.incubator)
    testImplementation(libs.robolectric)
}
