plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android instrumentation annotations"

android {
    namespace = "io.opentelemetry.android.instrumentation.annotations"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}
