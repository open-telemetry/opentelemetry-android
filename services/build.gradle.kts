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

    implementation(libs.androidx.core)
    compileOnly(libs.androidx.navigation.fragment)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.preference.ktx)

    testImplementation(libs.androidx.navigation.fragment)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
