plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

dependencies {
    // Pin at 3.0.0 for api compatibility
    api("com.squareup.okhttp3:okhttp:3.0.0")
    api("io.opentelemetry.instrumentation:opentelemetry-okhttp-3.0")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")
}
