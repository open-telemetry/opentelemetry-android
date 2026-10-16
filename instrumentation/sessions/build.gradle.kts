plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android sessions instrumentation"

android {
    kotlin {
        compilerOptions {
            optIn.add("io.opentelemetry.kotlin.ExperimentalApi")
        }
    }

    namespace = "io.opentelemetry.android.instrumentation.sessions"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    implementation(project(":agent-api"))
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":semconv"))
    implementation(libs.opentelemetry.kotlin.compat)
    implementation(libs.opentelemetry.semconv.kotlin)
    implementation(libs.opentelemetry.sdk)
    implementation(project(":core"))
    implementation(project(":session"))
}
