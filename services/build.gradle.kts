plugins {
    id("otel.android-library-conventions")
}

description = "OpenTelemetry android internal services"

android {
    namespace = "io.opentelemetry.android.internal.services"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":common"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.lifecycle.process)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
