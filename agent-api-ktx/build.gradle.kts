plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry Android Agent API Kotlin extensions"

android {
    namespace = "io.opentelemetry.android.agent.api.ktx"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(platform(libs.opentelemetry.platform.alpha)) // Required for sonatype publishing
    api(project(":agent-api"))
    api(libs.opentelemetry.kotlin.api)
    implementation(libs.opentelemetry.kotlin.compat)
}
