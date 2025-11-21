plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android Instrumentation Auto Service"

android {
    namespace = "io.opentelemetry.android.instrumentation"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":services"))
    implementation(project(":session"))

    implementation(project(":agent-api"))
}
