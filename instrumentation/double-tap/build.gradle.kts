plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android Double Tap gesture library instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.doubletap"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    implementation(project(":services"))
    implementation(project(":agent-api"))
    implementation(project(":instrumentation:android-instrumentation"))

    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.semconv.incubating)

    testImplementation(project(":test-common"))
    testImplementation(project(":session"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
