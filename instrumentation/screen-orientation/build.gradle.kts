plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android Screen Orientation instrumentation"

android {
    namespace = "io.opentelemetry.android.instrumentation.screenorientation"

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
}
