plugins {
    id("otel.android-library-conventions")
}

description = "OpenTelemetry Android common test utils"

android {
    namespace = "io.opentelemetry.android.test.common"
}

dependencies {
    api(project(":core"))
    api(platform(libs.opentelemetry.platform.alpha))
    api(libs.opentelemetry.sdk)
    api(libs.opentelemetry.api)
    api(libs.opentelemetry.sdk.testing)
    api(libs.assertj.core)
    implementation(libs.androidx.core)
    implementation(libs.androidx.junit)
}
