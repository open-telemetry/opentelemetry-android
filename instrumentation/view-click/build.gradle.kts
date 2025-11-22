plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android View click library instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.view.click"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":services"))
    implementation(project(":agent-api"))
    implementation(project(":instrumentation:android-instrumentation"))

    implementation(project(":core"))
    implementation(project(":session"))
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.semconv.incubating)
    implementation(libs.opentelemetry.api.incubator)

    testImplementation(project(":test-common"))
    testImplementation(project(":session"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
