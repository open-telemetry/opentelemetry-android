plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry build-time auto-instrumentation for OkHttp on Android"

android {
    namespace = "io.opentelemetry.android.okhttp.agent"

    defaultConfig {
        // OkHttp 5.5 requires consumers to compile against this.
        aarMetadata {
            minCompileSdk =
                libs.versions.minSupportedCompileSdk
                    .get()
                    .toInt()
        }
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(project(":instrumentation:okhttp3:library"))
    implementation(libs.byteBuddy)
}
