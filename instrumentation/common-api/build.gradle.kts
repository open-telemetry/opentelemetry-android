plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android ANR instrumentation common-api"

android {
    namespace = "io.opentelemetry.android.instrumentation.common"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    implementation(project(":agent-api"))
    implementation(project(":semconv"))
    implementation(project(":services"))
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.api)

    testImplementation(project(":test-common"))
    testImplementation(libs.androidx.fragment)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
