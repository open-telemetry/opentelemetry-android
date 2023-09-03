plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

extra["artifactId"] = "okhttp-3.0-library"

val otelVersion = project.property("otel.sdk.version")
dependencies {
    api("com.squareup.okhttp3:okhttp:3.0.0")
    api("io.opentelemetry.instrumentation:opentelemetry-okhttp-3.0:$otelVersion-alpha")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:$otelVersion-alpha")
}