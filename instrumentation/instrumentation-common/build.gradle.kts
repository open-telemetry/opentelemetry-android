plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android instrumentation common elements"

android {
    kotlin {
        compilerOptions {
            optIn.add("io.opentelemetry.kotlin.ExperimentalApi")
        }
    }

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
    implementation(project(":services"))

    testImplementation(project(":test-common"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
