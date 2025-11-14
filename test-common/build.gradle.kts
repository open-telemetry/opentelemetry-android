plugins {
    id("otel.android-library-conventions")
}

description = "OpenTelemetry Android common test utils"

android {
    namespace = "io.opentelemetry.android.test.common"

    packaging {
        resources.excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
    }
}

dependencies {
    implementation(project(":core"))
    api(project(":agent-api"))
    api(libs.opentelemetry.sdk)
    api(libs.opentelemetry.sdk.testing)
    api(libs.assertj.core)
    implementation(libs.androidx.core)
    implementation(libs.androidx.junit)
}
