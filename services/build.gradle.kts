plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
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

    api(libs.androidx.core)
    api(libs.androidx.navigation.runtime.ktx)
    api(libs.androidx.lifecycle.process)
    api(libs.androidx.preference.ktx)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.navigation.fragment)
    testImplementation(libs.androidx.junit.ktx)
}
