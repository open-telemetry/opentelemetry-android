plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android Power Save Mode instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.powersavemode"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":instrumentation:android-instrumentation"))
    implementation(project(":agent-api"))
    testImplementation(libs.robolectric)
}
