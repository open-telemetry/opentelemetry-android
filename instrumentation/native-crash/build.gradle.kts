plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android native crash instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.nativecrash"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    implementation(project(":agent-api"))
    implementation(project(":common"))
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":session"))
    implementation(libs.androidx.core)
    implementation(libs.opentelemetry.semconv.kotlin)

    testImplementation(project(":test-common"))
}
