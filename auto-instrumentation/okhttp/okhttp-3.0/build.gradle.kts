plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

val otelVersion = project.property("otel.sdk.version")
dependencies {
    implementation("io.opentelemetry.instrumentation:opentelemetry-okhttp-3.0:$otelVersion-alpha")
}