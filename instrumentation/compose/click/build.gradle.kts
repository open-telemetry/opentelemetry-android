plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android compose click instrumentation"

android {
    kotlin {
        compilerOptions {
            optIn.add("io.opentelemetry.kotlin.ExperimentalApi")
        }
    }

    namespace = "io.opentelemetry.android.instrumentation.compose.click"

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
    implementation(project(":services"))
    implementation(project(":instrumentation:instrumentation-common"))

    compileOnly(libs.compose) {
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "androidx.savedstate")
    }
    implementation(libs.opentelemetry.instrumentation.apiSemconv)

    testImplementation(project(":test-common"))
    testImplementation(project(":session"))

    testImplementation(libs.compose)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
