plugins {
    id("otel.java-library-conventions")
    id("otel.publish-conventions")
}

dependencies {
    implementation(platform(project(":dependencyManagement")))
    api("com.squareup.okhttp3:okhttp")
    api("io.opentelemetry.instrumentation:opentelemetry-okhttp-3.0")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")
}
