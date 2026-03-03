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
    kotlin {
        compilerOptions {
            optIn.add("io.opentelemetry.kotlin.ExperimentalApi")
            optIn.add("io.opentelemetry.kotlin.semconv.IncubatingApi")
        }
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    implementation(project(":services"))
    implementation(project(":agent-api"))
    implementation(project(":instrumentation:android-instrumentation"))

    implementation(libs.opentelemetry.kotlin.compat)
    implementation(libs.opentelemetry.kotlin.semconv)
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.semconv.incubating)

    testImplementation(project(":test-common"))
    testImplementation(project(":session"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
