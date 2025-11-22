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
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":common"))
    implementation(project(":services"))
    implementation(project(":session"))
    implementation(project(":core"))
    implementation(project(":instrumentation:common-api"))
    implementation(project(":agent-api"))
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.semconv.incubating)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.sdk.extension.incubator)
    implementation(libs.opentelemetry.instrumentation.api)
    testImplementation(libs.awaitility)
    testImplementation(libs.robolectric)
}
