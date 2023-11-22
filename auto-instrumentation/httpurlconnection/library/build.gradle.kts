plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

otelAndroid.minSdk = 24

description = "OpenTelemetry HttpURLConnection library instrumentation for Android"

dependencies {
    api(libs.opentelemetry.api)
    api(libs.opentelemetry.context)
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
    implementation(libs.opentelemetry.instrumentation.api)
}
