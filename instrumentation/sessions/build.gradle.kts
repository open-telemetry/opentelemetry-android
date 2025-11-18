plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android sessions instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.sessions"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":agent-api"))
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(libs.opentelemetry.api.incubator)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.semconv.incubating)
    implementation(project(":core"))
    implementation(project(":common"))
    implementation(project(":session"))
}
