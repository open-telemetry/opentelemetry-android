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
    api(project(":instrumentation:android-instrumentation"))
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
}
