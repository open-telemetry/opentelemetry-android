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
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":services"))
    implementation(project(":session"))
    implementation(project(":common"))
    implementation(project(":agent-api"))
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.semconv)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)
    testImplementation(libs.robolectric)
}
