plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry android internal network services"

android {
    namespace = "io.opentelemetry.android.internal.services.network"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    implementation(project(":common"))

    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.semconv.kotlin)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
