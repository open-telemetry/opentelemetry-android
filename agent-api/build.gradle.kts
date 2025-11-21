plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

android {
    namespace = "io.opentelemetry.android.agent.api"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.api)
}
