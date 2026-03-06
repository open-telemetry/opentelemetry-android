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
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    api(project(":session"))
    api(libs.opentelemetry.sdk)
}
