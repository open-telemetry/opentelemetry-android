plugins {
    id("otel.android-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry HttpURLConnection library instrumentation for Android"

android {
    namespace = "io.opentelemetry.android.httpurlconnection.library"

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    api(libs.opentelemetry.api)
    api(libs.opentelemetry.context)
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.instrumentation.api)
}
