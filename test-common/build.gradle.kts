plugins {
    id("otel.android-library-conventions")
}

description = "OpenTelemetry Android common test utils"

android {
    namespace = "io.opentelemetry.android.test.common"
}

dependencies {
    api(platform(libs.opentelemetry.platform))
    api(libs.opentelemetry.sdk)
    api(libs.opentelemetry.api)
    implementation(libs.androidx.core)
}
