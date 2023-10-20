plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

description = "OpenTelemetry OkHttp library instrumentation for Android"

dependencies {
    compileOnly(libs.okhttp)
    api(libs.opentelemetry.instrumentation.okhttp)
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
}
