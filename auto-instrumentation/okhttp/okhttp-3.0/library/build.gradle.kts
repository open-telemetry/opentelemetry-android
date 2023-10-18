plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

dependencies {
    compileOnly(libs.okhttp)
    api(libs.opentelemetry.instrumentation.okhttp)
    implementation(libs.opentelemetry.instrumentation.apiSemconv)
}
